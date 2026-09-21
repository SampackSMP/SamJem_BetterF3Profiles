package dev.betterf3pp.gui;

import dev.betterf3pp.misc.MiscLineKey;
import dev.betterf3pp.misc.MiscLineRegistry;
import dev.betterf3pp.misc.MiscSide;
import dev.betterf3pp.profile.F3Profile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

/**
 * One toggleable row per discovered misc line.
 *
 * <p>Rows come from two places: lines actually seen on the HUD this session, and lines the profile
 * already has a stored decision for. Without the second source, a line you hid in an earlier
 * session would be impossible to un-hide, because hiding it stops it from ever being seen again.
 */
public final class MiscLineListWidget extends ObjectSelectionList<MiscLineListWidget.LineEntry> {

    private final F3Profile profile;
    private final List<LineEntry> entries = new ArrayList<>();

    public MiscLineListWidget(final Minecraft minecraft, final F3Profile profile,
                              final int width, final int height, final int y, final int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.profile = profile;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 110;
    }

    /** Rebuilds rows from the registry plus the profile's stored decisions. */
    public void refresh() {
        this.entries.clear();
        this.clearEntries();

        for (final MiscSide side : MiscSide.values()) {
            final Map<String, MiscLineRegistry.Entry> seen = MiscLineRegistry.seen(side);
            final Map<String, String> labels = new LinkedHashMap<>();

            for (final Map.Entry<String, MiscLineRegistry.Entry> entry : seen.entrySet()) {
                labels.put(entry.getKey(), entry.getValue().label());
            }
            // Keys hidden in a previous session are not on screen, so they are not in the
            // registry; fold them in so they can still be re-enabled.
            for (final String key : this.profile.misc.decisions(side).keySet()) {
                labels.putIfAbsent(key, key);
            }

            for (final Map.Entry<String, String> entry : labels.entrySet()) {
                final MiscLineRegistry.Entry known = seen.get(entry.getKey());
                final LineEntry row = new LineEntry(side, entry.getKey(), entry.getValue(),
                        known != null ? known.sample() : null);
                this.entries.add(row);
                this.addEntry(row);
            }
        }
    }

    /** @return true when there is nothing to show */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** One misc line, with its visibility checkbox. */
    public final class LineEntry extends Entry<LineEntry> {

        private final MiscSide side;
        private final String key;
        private final String label;
        private final String sample;

        LineEntry(final MiscSide side, final String key, final String label, final String sample) {
            this.side = side;
            this.key = key;
            this.label = label;
            this.sample = sample;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.label);
        }

        @Override
        public void render(final GuiGraphics graphics, final int index, final int top, final int left,
                           final int entryWidth, final int entryHeight, final int mouseX, final int mouseY,
                           final boolean hovered, final float partialTick) {

            final Minecraft minecraft = Minecraft.getInstance();
            final boolean visible = MiscLineListWidget.this.profile.misc.visible(this.side, this.key);

            graphics.drawString(minecraft.font, visible ? "[x]" : "[ ]", left + 4, top + 2, 0xFFFFFF, true);

            final Component name = Component.literal(this.side == MiscSide.LEFT ? "L  " : "R  ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(this.label)
                            .withStyle(visible ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            graphics.drawString(minecraft.font, name, left + 28, top + 2, 0xFFFFFF, true);

            if (this.sample != null) {
                final String preview = MiscLineKey.stripFormatting(this.sample);
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(preview, entryWidth - 40),
                        left + 28, top + 14, 0x808080, true);
            }
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            final boolean visible = MiscLineListWidget.this.profile.misc.visible(this.side, this.key);
            MiscLineListWidget.this.profile.misc.setVisible(this.side, this.key, !visible);
            MiscLineListWidget.this.setSelected(this);
            return true;
        }
    }
}
