package com.commercesuite.catalog.service;

import java.text.Normalizer;

/** Shared slug helper for catalog (categories, brands, products). */
public final class CatalogSlug {
    private CatalogSlug() {}
    public static String slugify(String in) {
        if (in == null) return "";
        String n = Normalizer.normalize(in, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return n.length() > 160 ? n.substring(0, 160) : n;
    }
}