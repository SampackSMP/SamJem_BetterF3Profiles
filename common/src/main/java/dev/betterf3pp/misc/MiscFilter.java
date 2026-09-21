package dev.betterf3pp.misc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-profile visibility decisions for misc debug lines.
 *
 * <p>Decisions are stored explicitly as key to visible rather than as a set of hidden keys, so
 * the same structure serves both a blocklist ({@code hideNewByDefault == false}: everything shows
 * until you hide it) and an allowlist ({@code hideNewByDefault == true}: nothing shows until you
 * reveal it).</p>
 */
public final class MiscFilter {

    /** When true, a line with no recorded decision is hidden instead of shown. */
    public boolean hideNewByDefault;

    private final Map<String, Boolean> left = new LinkedHashMap<>();
    private final Map<String, Boolean> right = new LinkedHashMap<>();

    public Map<String, Boolean> decisions(final MiscSide side) {
        return side == MiscSide.LEFT ? this.left : this.right;
    }

    /**
     * Whether a line should be rendered.
     *
     * @param side the side the line belongs to
     * @param normalizedKey the key, already passed through {@link MiscLineKey#normalize}
     * @return true if the line should be drawn
     */
    public boolean visible(final MiscSide side, final String normalizedKey) {
        final Boolean decision = this.decisions(side).get(normalizedKey);
        return decision != null ? decision : !this.hideNewByDefault;
    }

    public void setVisible(final MiscSide side, final String normalizedKey, final boolean visible) {
        this.decisions(side).put(normalizedKey, visible);
    }

    public void clearDecisions() {
        this.left.clear();
        this.right.clear();
    }

    /**
     * Deep-copies this filter, for profile duplication.
     *
     * @return an independent copy
     */
    public MiscFilter copy() {
        final MiscFilter copy = new MiscFilter();
        copy.hideNewByDefault = this.hideNewByDefault;
        copy.left.putAll(this.left);
        copy.right.putAll(this.right);
        return copy;
    }
}
