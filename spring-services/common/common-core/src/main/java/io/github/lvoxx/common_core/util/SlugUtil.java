package io.github.lvoxx.common_core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern ACCENTS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SlugUtil() {
    }

    /**
     * Convert string to URL-friendly slug
     * "Hello World!" → "hello-world"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Convert to lowercase
        String slug = input.toLowerCase(Locale.ENGLISH);

        // Remove accents
        String nfd = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = ACCENTS.matcher(nfd).replaceAll("");

        // Replace spaces and underscores with hyphens
        slug = slug.replaceAll("[\\s_]+", "-");

        // Remove all non-alphanumeric characters except hyphens
        slug = slug.replaceAll("[^a-z0-9\\-]", "");

        // Remove leading/trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        // Replace multiple consecutive hyphens with single hyphen
        slug = slug.replaceAll("-{2,}", "-");

        return slug;
    }

    /**
     * Validate if string is a valid slug format
     */
    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return SLUG_PATTERN.matcher(slug).matches();
    }
}
