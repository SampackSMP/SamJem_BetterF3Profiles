package dev.betterf3pp.misc;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers which misc lines have actually been seen this session, so the config screen can
 * offer real checkboxes instead of asking the user to type keys.
 *
 * <p>Entries keep insertion order, which is the order the lines appear on the HUD. The map is
 * capped because the key derivation cannot be perfect for every mod's output, and an unbounded
 * map fed from a render-loop callback would be a slow leak.</p>
 */
public final class MiscLineRegistry {

    /** Upper bound on remembered keys per side. */
    public static final int MAX_ENTRIES = 256;

    private static final Map<MiscSide, LinkedHashMap<String, Entry>> SEEN = new EnumMap<>(MiscSide.class);

    static {
        for (final MiscSide side : MiscSide.values()) {
            SEEN.put(side, new LinkedHashMap<>());
        }
    }

    private MiscLineRegistry() {
        // Utility class
    }

    /**
     * Records that a line was rendered.
     *
     * @param side the side it appeared on
     * @param normalizedKey the lookup key
     * @param label the key with its original casing, for display
     * @param sample the full raw line, for a live preview in the config screen
     */
    public static synchronized void record(final MiscSide side, final String normalizedKey,
                                           final String label, final String sample) {
        final LinkedHashMap<String, Entry> map = SEEN.get(side);
        final Entry existing = map.get(normalizedKey);
        if (existing != null) {
            existing.sample = sample;
            return;
        }
        if (map.size() >= MAX_ENTRIES) {
            return;
        }
        map.put(normalizedKey, new Entry(label, sample));
    }

    /**
     * Snapshot of everything seen on a side.
     *
     * @param side the side to read
     * @return an immutable copy, in screen order
     */
    public static synchronized Map<String, Entry> seen(final MiscSide side) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(SEEN.get(side)));
    }

    public static synchronized void clear() {
        for (final LinkedHashMap<String, Entry> map : SEEN.values()) {
            map.clear();
        }
    }

    /** A remembered line: how to label it, and what it last looked like. */
    public static final class Entry {
        private final String label;
        private String sample;

        Entry(final String label, final String sample) {
            this.label = label;
            this.sample = sample;
        }

        public String label() {
            return this.label;
        }

        public String sample() {
            return this.sample;
        }
    }
}
