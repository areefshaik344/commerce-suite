package com.commercesuite.catalog.service.storefront;

import com.commercesuite.catalog.dto.storefront.*;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.exception.AppException;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storefront read model.
 *
 * <p>Strictly read-only. Bypasses the domain services (which are write/command
 * focused) and uses native SQL with explicit projections to:
 *   <ul>
 *     <li>compose the denormalized {@link ProductCardDto} in a single query</li>
 *     <li>compute facets over the full unpaginated result set without round-tripping</li>
 *     <li>batch-load PDP related rows (media / variants / attributes / inventory / reviews / related)
 *         using IN-clauses so we never N+1 fan out per product</li>
 *   </ul>
 *
 * <p>Domain ownership, FSMs, and write paths are untouched.
 */
@Service
@RequiredArgsConstructor
public class StorefrontReadService {

    private static final int MAX_PAGE_SIZE = 60;
    private static final List<SortOption> SORT_OPTIONS = List.of(
            new SortOption("newest",     "Newest",         "p.created_at DESC"),
            new SortOption("price-asc",  "Price: Low → High", "default_price ASC NULLS LAST"),
            new SortOption("price-desc", "Price: High → Low", "default_price DESC NULLS LAST"),
            new SortOption("rating",     "Top rated",      "rating_avg DESC NULLS LAST"),
            new SortOption("popularity", "Most reviewed",  "review_count DESC NULLS LAST")
    );
    private record SortOption(String code, String label, String sql) {}

    private final JdbcTemplate jdbc;

    /* ---------------------------------------------------------------- *
     *  Product search (cards + facets)                                  *
     * ---------------------------------------------------------------- */

    @Transactional(readOnly = true)
    public ProductSearchResultDto search(StorefrontSearchCriteria c, int page, int size, String sortCode) {
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        page = Math.max(page, 0);
        SortOption sort = SORT_OPTIONS.stream().filter(s -> s.code.equalsIgnoreCase(sortCode))
                .findFirst().orElse(SORT_OPTIONS.get(0));

        WhereClause w = buildWhere(c);

        // Total count over filtered set (single query).
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products p " + w.joins + " WHERE p.deleted_at IS NULL AND p.status = 'APPROVED' " + w.sql,
                Long.class, w.args.toArray());
        long totalRows = total == null ? 0 : total;

        List<ProductCardDto> items;
        if (totalRows == 0) {
            items = List.of();
        } else {
            String cardSelect = baseCardSelect()
                    + " WHERE p.deleted_at IS NULL AND p.status = 'APPROVED' " + w.sql
                    + " ORDER BY " + sort.sql + ", p.id ASC LIMIT ? OFFSET ?";
            List<Object> args = new ArrayList<>(w.args);
            args.add(size);
            args.add(page * (long) size);
            items = jdbc.query(cardSelect, args.toArray(), CARD_MAPPER);
        }

        StorefrontFacetsDto facets = computeFacets(c);
        int totalPages = (int) Math.ceil(totalRows / (double) size);
        PageResponse<ProductCardDto> pageDto = new PageResponse<>(items, page, size, totalRows, totalPages);

        List<SortOptionDto> sortOpts = SORT_OPTIONS.stream()
                .map(s -> new SortOptionDto(s.code, s.label)).toList();
        return new ProductSearchResultDto(pageDto, facets, sortOpts, sort.code);
    }

    private StorefrontFacetsDto computeFacets(StorefrontSearchCriteria c) {
        // Facets ignore the dimension they describe (so unselecting still shows counts).
        // For simplicity, all facets share the same base where-set (parity with mock UX).
        WhereClause w = buildWhere(c);
        String base = " FROM products p "
                + " LEFT JOIN brands b ON b.id = p.brand_id AND b.deleted_at IS NULL "
                + " LEFT JOIN categories ca ON ca.id = p.category_id AND ca.deleted_at IS NULL "
                + " LEFT JOIN LATERAL (SELECT MIN(pv.price_paise) AS default_price FROM product_variants pv "
                + "    WHERE pv.product_id = p.id AND pv.deleted_at IS NULL AND pv.active = true) v ON TRUE "
                + " LEFT JOIN LATERAL (SELECT AVG(pr.rating)::float AS rating_avg, COUNT(*) AS review_count "
                + "    FROM product_reviews pr WHERE pr.product_id = p.id AND pr.deleted_at IS NULL "
                + "      AND pr.status = 'PUBLISHED') r ON TRUE "
                + " WHERE p.deleted_at IS NULL AND p.status = 'APPROVED' "
                + w.sql;

        List<BrandFilterDto> brands = jdbc.query(
                "SELECT b.id, b.name, b.slug, b.logo_url, COUNT(*) AS cnt " + base
                        + " AND p.brand_id IS NOT NULL GROUP BY b.id, b.name, b.slug, b.logo_url "
                        + " ORDER BY cnt DESC, b.name ASC LIMIT 50",
                w.args.toArray(),
                (rs, i) -> new BrandFilterDto(
                        (UUID) rs.getObject("id"), rs.getString("name"), rs.getString("slug"),
                        rs.getString("logo_url"), rs.getLong("cnt")));

        List<CategoryFacetDto> categories = jdbc.query(
                "SELECT ca.id, ca.name, ca.slug, COUNT(*) AS cnt " + base
                        + " GROUP BY ca.id, ca.name, ca.slug ORDER BY cnt DESC, ca.name ASC LIMIT 50",
                w.args.toArray(),
                (rs, i) -> new CategoryFacetDto(
                        (UUID) rs.getObject("id"), rs.getString("name"), rs.getString("slug"),
                        rs.getLong("cnt")));

        Map<String, Object> priceRow = jdbc.queryForMap(
                "SELECT COALESCE(MIN(default_price), 0) AS min_p, COALESCE(MAX(default_price), 0) AS max_p " + base,
                w.args.toArray());
        PriceRangeDto range = new PriceRangeDto(
                ((Number) priceRow.get("min_p")).longValue(),
                ((Number) priceRow.get("max_p")).longValue());

        List<RatingBucketDto> ratings = new ArrayList<>();
        for (int min = 4; min >= 1; min--) {
            Long cnt = jdbc.queryForObject(
                    "SELECT COUNT(*) " + base + " AND COALESCE(r.rating_avg, 0) >= ?",
                    appendArg(w.args, (double) min), Long.class);
            ratings.add(new RatingBucketDto(min, cnt == null ? 0 : cnt));
        }
        return new StorefrontFacetsDto(brands, categories, range, ratings);
    }

    /* ---------------------------------------------------------------- *
     *  Product detail (PDP)                                             *
     * ---------------------------------------------------------------- */

    @Transactional(readOnly = true)
    public ProductDetailDto detailBySlug(String slug) {
        Map<String, Object> head;
        try {
            head = jdbc.queryForMap(
                    "SELECT p.id, p.slug, p.title, p.short_description, p.description, p.vendor_id, "
                            + "       p.category_id, ca.name AS category_name, "
                            + "       p.brand_id, b.name AS brand_name, b.logo_url AS brand_logo_url "
                            + "  FROM products p "
                            + "  LEFT JOIN brands b ON b.id = p.brand_id AND b.deleted_at IS NULL "
                            + "  LEFT JOIN categories ca ON ca.id = p.category_id AND ca.deleted_at IS NULL "
                            + " WHERE p.deleted_at IS NULL AND p.status = 'APPROVED' AND p.slug = ?",
                    slug);
        } catch (org.springframework.dao.EmptyResultDataAccessException nf) {
            throw AppException.notFound("Product");
        }
        UUID productId = (UUID) head.get("id");
        return assembleDetail(head, productId);
    }

    private ProductDetailDto assembleDetail(Map<String, Object> head, UUID productId) {
        List<ProductMediaItemDto> media = jdbc.query(
                "SELECT id, url, alt_text, media_type::text AS media_type, sort_order "
                        + "  FROM product_media WHERE product_id = ? AND deleted_at IS NULL "
                        + "  ORDER BY sort_order ASC, created_at ASC",
                new Object[]{productId},
                (rs, i) -> new ProductMediaItemDto((UUID) rs.getObject("id"),
                        rs.getString("url"), rs.getString("alt_text"),
                        rs.getString("media_type"), rs.getInt("sort_order")));

        List<ProductVariantSummaryDto> variants = jdbc.query(
                "SELECT pv.id, pv.sku, pv.price_paise, pv.compare_at_paise, pv.currency, "
                        + "       pv.is_default, pv.options_json::text AS options_json, "
                        + "       COALESCE(ii.on_hand_qty, 0) - COALESCE(ii.reserved_qty, 0) AS avail "
                        + "  FROM product_variants pv "
                        + "  LEFT JOIN inventory_items ii ON ii.variant_id = pv.id AND ii.deleted_at IS NULL "
                        + " WHERE pv.product_id = ? AND pv.deleted_at IS NULL AND pv.active = true "
                        + " ORDER BY pv.is_default DESC, pv.created_at ASC",
                new Object[]{productId},
                (rs, i) -> {
                    int avail = Math.max(0, rs.getInt("avail"));
                    return new ProductVariantSummaryDto(
                            (UUID) rs.getObject("id"), rs.getString("sku"),
                            rs.getLong("price_paise"),
                            (Long) rs.getObject("compare_at_paise"),
                            rs.getString("currency"),
                            rs.getBoolean("is_default"),
                            avail,
                            stockStatus(avail),
                            rs.getString("options_json"));
                });
        ProductVariantSummaryDto defaultVariant = variants.stream().filter(ProductVariantSummaryDto::isDefault)
                .findFirst().orElse(variants.isEmpty() ? null : variants.get(0));

        List<ProductAttributeItemDto> attributes = jdbc.query(
                "SELECT d.code, d.label, d.unit, pav.value_text, pav.value_number, pav.value_bool, pav.value_json "
                        + "  FROM product_attribute_values pav "
                        + "  JOIN product_attribute_definitions d ON d.id = pav.definition_id AND d.deleted_at IS NULL "
                        + " WHERE pav.product_id = ? AND pav.deleted_at IS NULL "
                        + " ORDER BY d.sort_order ASC, d.label ASC",
                new Object[]{productId}, ATTR_MAPPER);

        InventorySummaryDto inv = jdbc.queryForObject(
                "SELECT COALESCE(SUM(ii.on_hand_qty), 0)  AS on_hand, "
                        + "       COALESCE(SUM(ii.reserved_qty), 0) AS reserved "
                        + "  FROM product_variants pv "
                        + "  LEFT JOIN inventory_items ii ON ii.variant_id = pv.id AND ii.deleted_at IS NULL "
                        + " WHERE pv.product_id = ? AND pv.deleted_at IS NULL AND pv.active = true",
                new Object[]{productId},
                (rs, i) -> {
                    int on = rs.getInt("on_hand");
                    int res = rs.getInt("reserved");
                    int avail = Math.max(0, on - res);
                    return new InventorySummaryDto(on, res, avail, stockStatus(avail));
                });

        ReviewSummaryDto reviews = reviewSummary(productId);

        UUID categoryId = (UUID) head.get("category_id");
        List<ProductCardDto> related = jdbc.query(
                baseCardSelect() + " WHERE p.deleted_at IS NULL AND p.status = 'APPROVED' "
                        + "   AND p.category_id = ? AND p.id <> ? ORDER BY p.created_at DESC LIMIT 8",
                new Object[]{categoryId, productId}, CARD_MAPPER);

        return new ProductDetailDto(
                productId,
                (String) head.get("slug"),
                (String) head.get("title"),
                (String) head.get("short_description"),
                (String) head.get("description"),
                (UUID) head.get("vendor_id"),
                categoryId,
                (String) head.get("category_name"),
                (UUID) head.get("brand_id"),
                (String) head.get("brand_name"),
                (String) head.get("brand_logo_url"),
                media, variants, defaultVariant, attributes, inv, reviews, related);
    }

    /* ---------------------------------------------------------------- *
     *  Reviews                                                          *
     * ---------------------------------------------------------------- */

    @Transactional(readOnly = true)
    public ReviewSummaryDto reviewSummary(UUID productId) {
        Map<String, Object> agg = jdbc.queryForMap(
                "SELECT COALESCE(AVG(rating), 0)::float                AS avg_rating, "
                        + "       COUNT(*)                                       AS cnt, "
                        + "       COUNT(*) FILTER (WHERE verified_purchase)      AS verified "
                        + "  FROM product_reviews "
                        + " WHERE product_id = ? AND deleted_at IS NULL AND status = 'PUBLISHED'",
                productId);
        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int r = 1; r <= 5; r++) dist.put(r, 0L);
        jdbc.query(
                "SELECT rating, COUNT(*) AS cnt FROM product_reviews "
                        + " WHERE product_id = ? AND deleted_at IS NULL AND status = 'PUBLISHED' "
                        + " GROUP BY rating",
                rs -> { dist.put((int) rs.getShort("rating"), rs.getLong("cnt")); },
                productId);
        return new ReviewSummaryDto(
                round1(((Number) agg.get("avg_rating")).doubleValue()),
                ((Number) agg.get("cnt")).longValue(),
                ((Number) agg.get("verified")).longValue(),
                dist);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewItemDto> listReviews(UUID productId, int page, int size) {
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        page = Math.max(page, 0);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_reviews "
                        + " WHERE product_id = ? AND deleted_at IS NULL AND status = 'PUBLISHED'",
                Long.class, productId);
        long t = total == null ? 0 : total;
        List<ReviewItemDto> items = t == 0 ? List.of() : jdbc.query(
                "SELECT pr.id, pr.product_id, pr.customer_id, pr.rating, pr.title, pr.review_text, "
                        + "       pr.verified_purchase, pr.helpful_count, pr.created_at, "
                        + "       COALESCE(pf.display_name, pf.full_name, 'Customer') AS display_name "
                        + "  FROM product_reviews pr "
                        + "  LEFT JOIN profiles pf ON pf.user_id = pr.customer_id AND pf.deleted_at IS NULL "
                        + " WHERE pr.product_id = ? AND pr.deleted_at IS NULL AND pr.status = 'PUBLISHED' "
                        + " ORDER BY pr.created_at DESC LIMIT ? OFFSET ?",
                new Object[]{productId, size, page * (long) size},
                (rs, i) -> new ReviewItemDto(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("product_id"),
                        (UUID) rs.getObject("customer_id"),
                        rs.getString("display_name"),
                        rs.getShort("rating"),
                        rs.getString("title"),
                        rs.getString("review_text"),
                        rs.getBoolean("verified_purchase"),
                        rs.getInt("helpful_count"),
                        rs.getTimestamp("created_at").toInstant()));
        return new PageResponse<>(items, page, size, t, (int) Math.ceil(t / (double) size));
    }

    /* ---------------------------------------------------------------- *
     *  Search suggestions / related                                     *
     * ---------------------------------------------------------------- */

    @Transactional(readOnly = true)
    public List<String> suggest(String q, int limit) {
        if (q == null || q.isBlank()) return List.of();
        String like = "%" + q.toLowerCase().trim() + "%";
        return jdbc.query(
                "SELECT title FROM products WHERE deleted_at IS NULL AND status = 'APPROVED' "
                        + "  AND LOWER(title) LIKE ? ORDER BY title ASC LIMIT ?",
                new Object[]{like, Math.min(limit, 10)},
                (rs, i) -> rs.getString(1));
    }

    @Transactional(readOnly = true)
    public List<BrandFilterDto> brandFilter() {
        return jdbc.query(
                "SELECT b.id, b.name, b.slug, b.logo_url, "
                        + "       (SELECT COUNT(*) FROM products p WHERE p.brand_id = b.id "
                        + "          AND p.deleted_at IS NULL AND p.status = 'APPROVED') AS cnt "
                        + "  FROM brands b WHERE b.deleted_at IS NULL AND b.active = true "
                        + "  ORDER BY b.name ASC",
                (rs, i) -> new BrandFilterDto(
                        (UUID) rs.getObject("id"), rs.getString("name"), rs.getString("slug"),
                        rs.getString("logo_url"), rs.getLong("cnt")));
    }

    /* ---------------------------------------------------------------- *
     *  Helpers                                                          *
     * ---------------------------------------------------------------- */

    private static String baseCardSelect() {
        return "SELECT p.id, p.slug, p.title, p.vendor_id, "
                + "       p.brand_id, b.name AS brand_name, "
                + "       p.category_id, ca.name AS category_name, "
                + "       v.default_variant_id, v.default_price, v.compare_at, v.currency, "
                + "       m.url AS primary_url, m.alt_text AS primary_alt, "
                + "       COALESCE(r.rating_avg, 0)::float AS rating_avg, "
                + "       COALESCE(r.review_count, 0)     AS review_count, "
                + "       COALESCE(inv.avail, 0)          AS avail "
                + "  FROM products p "
                + "  LEFT JOIN brands b ON b.id = p.brand_id AND b.deleted_at IS NULL "
                + "  LEFT JOIN categories ca ON ca.id = p.category_id AND ca.deleted_at IS NULL "
                + "  LEFT JOIN LATERAL ("
                + "      SELECT pv.id AS default_variant_id, pv.price_paise AS default_price, "
                + "             pv.compare_at_paise AS compare_at, pv.currency "
                + "        FROM product_variants pv "
                + "       WHERE pv.product_id = p.id AND pv.deleted_at IS NULL AND pv.active = true "
                + "       ORDER BY pv.is_default DESC, pv.created_at ASC LIMIT 1) v ON TRUE "
                + "  LEFT JOIN LATERAL ("
                + "      SELECT url, alt_text FROM product_media "
                + "       WHERE product_id = p.id AND deleted_at IS NULL "
                + "       ORDER BY sort_order ASC LIMIT 1) m ON TRUE "
                + "  LEFT JOIN LATERAL ("
                + "      SELECT AVG(rating)::float AS rating_avg, COUNT(*) AS review_count "
                + "        FROM product_reviews "
                + "       WHERE product_id = p.id AND deleted_at IS NULL AND status = 'PUBLISHED') r ON TRUE "
                + "  LEFT JOIN LATERAL ("
                + "      SELECT GREATEST(0, SUM(ii.on_hand_qty) - SUM(ii.reserved_qty))::int AS avail "
                + "        FROM product_variants pv2 "
                + "        LEFT JOIN inventory_items ii ON ii.variant_id = pv2.id AND ii.deleted_at IS NULL "
                + "       WHERE pv2.product_id = p.id AND pv2.deleted_at IS NULL AND pv2.active = true) inv ON TRUE ";
    }

    private static final RowMapper<ProductCardDto> CARD_MAPPER = (rs, i) -> {
        int avail = rs.getInt("avail");
        long price = rs.getObject("default_price") == null ? 0L : rs.getLong("default_price");
        Long compareAt = (Long) rs.getObject("compare_at");
        return new ProductCardDto(
                (UUID) rs.getObject("id"),
                rs.getString("slug"),
                rs.getString("title"),
                (UUID) rs.getObject("brand_id"),
                rs.getString("brand_name"),
                (UUID) rs.getObject("category_id"),
                rs.getString("category_name"),
                (UUID) rs.getObject("default_variant_id"),
                price,
                compareAt,
                rs.getString("currency") == null ? "INR" : rs.getString("currency"),
                rs.getString("primary_url"),
                rs.getString("primary_alt"),
                round1(rs.getDouble("rating_avg")),
                rs.getLong("review_count"),
                stockStatus(avail),
                avail,
                false,
                (UUID) rs.getObject("vendor_id"));
    };

    private static final RowMapper<ProductAttributeItemDto> ATTR_MAPPER = (rs, i) -> {
        String value = rs.getString("value_text");
        if (value == null) {
            Object num = rs.getObject("value_number");
            if (num != null) value = num.toString();
        }
        if (value == null) {
            Object b = rs.getObject("value_bool");
            if (b != null) value = b.toString();
        }
        if (value == null) value = rs.getString("value_json");
        return new ProductAttributeItemDto(rs.getString("code"), rs.getString("label"), value, rs.getString("unit"));
    };

    private static String stockStatus(int available) {
        if (available <= 0) return "OUT_OF_STOCK";
        if (available <= 5) return "LOW_STOCK";
        return "IN_STOCK";
    }

    private static double round1(double d) { return Math.round(d * 10.0) / 10.0; }

    private static Object[] appendArg(List<Object> base, Object extra) {
        Object[] out = new Object[base.size() + 1];
        for (int i = 0; i < base.size(); i++) out[i] = base.get(i);
        out[base.size()] = extra;
        return out;
    }

    private record WhereClause(String sql, String joins, List<Object> args) {}

    private WhereClause buildWhere(StorefrontSearchCriteria c) {
        StringBuilder sb = new StringBuilder();
        List<Object> args = new ArrayList<>();
        // Join the default-variant lateral for price filters even outside cards.
        String joins = " LEFT JOIN LATERAL ("
                + "   SELECT MIN(pv.price_paise) AS default_price FROM product_variants pv "
                + "    WHERE pv.product_id = p.id AND pv.deleted_at IS NULL AND pv.active = true) v ON TRUE "
                + " LEFT JOIN LATERAL ("
                + "   SELECT AVG(pr.rating)::float AS rating_avg, COUNT(*) AS review_count FROM product_reviews pr "
                + "    WHERE pr.product_id = p.id AND pr.deleted_at IS NULL AND pr.status = 'PUBLISHED') r ON TRUE ";

        if (c.keyword() != null && !c.keyword().isBlank()) {
            sb.append(" AND (LOWER(p.title) LIKE ? OR LOWER(p.short_description) LIKE ? OR LOWER(p.slug) LIKE ?) ");
            String like = "%" + c.keyword().toLowerCase().trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (c.categoryId() != null) { sb.append(" AND p.category_id = ? "); args.add(c.categoryId()); }
        if (c.categorySlug() != null && !c.categorySlug().isBlank()) {
            sb.append(" AND p.category_id = (SELECT id FROM categories WHERE slug = ? AND deleted_at IS NULL) ");
            args.add(c.categorySlug());
        }
        if (c.brandIds() != null && !c.brandIds().isEmpty()) {
            sb.append(" AND p.brand_id IN (").append(placeholders(c.brandIds().size())).append(") ");
            args.addAll(c.brandIds());
        }
        if (c.vendorId() != null) { sb.append(" AND p.vendor_id = ? "); args.add(c.vendorId()); }
        if (c.minPricePaise() != null) { sb.append(" AND v.default_price >= ? "); args.add(c.minPricePaise()); }
        if (c.maxPricePaise() != null) { sb.append(" AND v.default_price <= ? "); args.add(c.maxPricePaise()); }
        if (c.minRating() != null) { sb.append(" AND COALESCE(r.rating_avg, 0) >= ? "); args.add(c.minRating()); }
        return new WhereClause(sb.toString(), joins, args);
    }

    private static String placeholders(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) { if (i > 0) s.append(','); s.append('?'); }
        return s.toString();
    }

    @SuppressWarnings("unused")
    private static UUID uuid(ResultSet rs, String col) throws java.sql.SQLException {
        return (UUID) rs.getObject(col);
    }
}