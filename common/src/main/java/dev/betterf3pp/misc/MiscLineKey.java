package dev.betterf3pp.misc;

import java.util.Locale;

/**
 * Derives a stable identity for a misc debug line.
 *
 * <p>Misc lines are raw strings whose values change every frame ({@code "Chunks: 128/512"} one
 * tick, {@code "Chunks: 129/512"} the next), so the user's choice to hide one has to be keyed on
 * something that survives those changes. The rules mirror how BetterF3 itself splits these lines
 * in {@code Utils.formattedFromString}: everything before the first colon is the name, the rest
 * is the value.</p>
 */
public final class MiscLineKey {

    /** Keys are truncated to this length so a pathological line cannot bloat the config. */
    public static final int MAX_LENGTH = 64;

    private MiscLineKey() {
        // Utility class
    }

    /**
     * Derives the display key for a raw misc line.
     *
     * @param raw the line as BetterF3 received it, possibly containing formatting codes
     * @return a stable key, or an empty string if the line carries no identity
     */
    public static String of(final String raw) {
        if (raw == null) {
            return "";
        }
        final String stripped = stripFormatting(raw).trim();
        if (stripped.isEmpty()) {
            return "";
        }

        final int colon = stripped.indexOf(':');
        String key = colon > 0 ? stripped.substring(0, colon).trim() : "";
        if (key.isEmpty()) {
            // No usable name half - fall back to the whole line with its numbers neutralised.
            key = collapseDigits(stripped);
        }

        if (key.length() > MAX_LENGTH) {
            key = key.substring(0, MAX_LENGTH);
        }
        return key;
    }

    /**
     * Normalises a key for storage and lookup. Display keeps the original casing.
     *
     * @param key a key produced by {@link #of(String)}
     * @return the lookup form of that key
     */
    public static String normalize(final String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    /**
     * Removes Minecraft formatting codes ({@code §} followed by one character).
     *
     * @param text the text to strip
     * @return the text without formatting codes
     */
    public static String stripFormatting(final String text) {
        return text.replaceAll("\u00a7.", "");
    }

    /**
     * Replaces every run of digits with {@code #} so that counters stay identifiable.
     *
     * @param text the text to neutralise
     * @return the text with digit runs collapsed
     */
    public static String collapseDigits(final String text) {
        return text.replaceAll("\\d+", "#").trim();
    }
}
