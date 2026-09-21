package dev.betterf3pp.misc;

import dev.betterf3pp.profile.ProfileManager;
import java.util.ArrayList;
import java.util.List;

/**
 * The single place misc lines are inspected and dropped.
 *
 * <p>Called from the {@code DebugLineList.values} mixin, which BetterF3's two misc modules both
 * funnel through on every loader.</p>
 */
public final class MiscLineFilter {

    private MiscLineFilter() {
        // Utility class
    }

    /**
     * Records and filters one frame's worth of misc lines.
     *
     * @param lineListId the id of the {@code DebugLineList} being updated
     * @param values the lines BetterF3 wants to display
     * @return the lines that should actually be displayed
     */
    public static List<String> apply(final String lineListId, final List<String> values) {
        final MiscSide side = MiscSide.fromLineId(lineListId);
        if (side == null || values == null || values.isEmpty()) {
            return values;
        }

        final MiscFilter filter = ProfileManager.activeFilter();
        final List<String> kept = new ArrayList<>(values.size());

        for (final String raw : values) {
            final String label = MiscLineKey.of(raw);
            if (label.isEmpty()) {
                // Blank or formatting-only line: BetterF3's own spacing, leave it alone.
                kept.add(raw);
                continue;
            }
            final String key = MiscLineKey.normalize(label);
            MiscLineRegistry.record(side, key, label, raw);

            if (filter == null || filter.visible(side, key)) {
                kept.add(raw);
            }
        }
        return kept;
    }
}
