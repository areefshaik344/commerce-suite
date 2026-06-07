package com.commercesuite.vendor.service;

import com.commercesuite.vendor.repository.VendorProfileRepository;
import java.text.Normalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlugGenerator {
    private final VendorProfileRepository profileRepo;

    public String uniqueSlug(String base) {
        String slug = slugify(base);
        if (slug.isEmpty()) slug = "store";
        String candidate = slug;
        int i = 1;
        while (profileRepo.existsByStoreSlug(candidate)) {
            i++;
            candidate = slug + "-" + i;
            if (i > 9999) throw new IllegalStateException("Slug exhaustion for " + base);
        }
        return candidate;
    }

    private static String slugify(String in) {
        String n = Normalizer.normalize(in, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return n.length() > 120 ? n.substring(0, 120) : n;
    }
}