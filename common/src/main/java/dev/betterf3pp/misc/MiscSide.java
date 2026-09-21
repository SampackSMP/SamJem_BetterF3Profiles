package dev.betterf3pp.misc;

import org.jetbrains.annotations.Nullable;

/**
 * Which of BetterF3's two misc modules a scraped line came from.
 *
 * <p>BetterF3 gives the two {@code DebugLineList} instances the ids {@code misc_left} and
 * {@code misc_right}, and nothing else in the mod constructs one, so the id is an
 * unambiguous discriminator.</p>
 */
public enum MiscSide {
    LEFT("misc_left"),
    RIGHT("misc_right");

    private final String lineId;

    MiscSide(final String lineId) {
        this.lineId = lineId;
    }

    public String lineId() {
        return this.lineId;
    }

    /**
     * Resolves a {@code DebugLineList} id to a side.
     *
     * @param lineId the line list id
     * @return the matching side, or {@code null} if this is not one of BetterF3's misc lists
     */
    @Nullable
    public static MiscSide fromLineId(@Nullable final String lineId) {
        if (LEFT.lineId.equals(lineId)) {
            return LEFT;
        }
        if (RIGHT.lineId.equals(lineId)) {
            return RIGHT;
        }
        return null;
    }
}
