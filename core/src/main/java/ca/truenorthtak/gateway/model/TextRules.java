package ca.truenorthtak.gateway.model;

/**
 * Free-text normalisation shared by all codecs. Text is upper-cased and limited to the
 * 64-character DEC SIXBIT set (ASCII 32-95) minus '/', which is the field delimiter in
 * character-oriented messages. This mirrors the ADatP-3 convention and lets the compact
 * binary codec use 6 bits per character.
 */
public final class TextRules {
    private TextRules() {}

    public static String normalize(String text) {
        return text == null ? null : text.toUpperCase(java.util.Locale.ROOT).trim();
    }

    public static boolean isAllowed(char c) {
        return c >= 32 && c <= 95 && c != '/';
    }

    public static void check(String field, String text, int max) {
        if (text.length() > max) {
            throw new ValidationException(field + ": text longer than " + max);
        }
        for (int k = 0; k < text.length(); k++) {
            if (!isAllowed(text.charAt(k))) {
                throw new ValidationException(field + ": character '" + text.charAt(k) + "' not allowed");
            }
        }
    }
}
