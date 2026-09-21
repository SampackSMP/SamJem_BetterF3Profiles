package dev.betterf3pp.gui;

import dev.betterf3pp.profile.F3Profile;
import dev.betterf3pp.profile.ProfileManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

/**
 * Scrollable list of profiles, in tab order.
 *
 * <p>Reordering follows the gesture BetterF3 already uses in its own module list: clicking the
 * left gutter of a row moves it up or down depending on which half you hit. Arrows are drawn as
 * text rather than GUI sprites, which keeps this free of resource-name assumptions.
 */
public final class ProfileListWidget extends ObjectSelectionList<ProfileListWidget.ProfileEntry> {

    /** Width of the click target that reorders rows. */
    private static final int GUTTER = 24;

    private final ProfilesScreen parent;
    private final List<ProfileEntry> entries = new ArrayList<>();

    public ProfileListWidget(final ProfilesScreen parent, final Minecraft minecraft,
                             final int width, final int height, final int y, final int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.parent = parent;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 85;
    }

    /** Rebuilds the rows from the current profile list, preserving the selected profile. */
    public void refresh() {
        final F3Profile selected = this.getSelected() != null ? this.getSelected().profile : null;
        this.entries.clear();
        this.clearEntries();
        for (final F3Profile profile : ProfileManager.profiles()) {
            final ProfileEntry entry = new ProfileEntry(profile);
            this.entries.add(entry);
            this.addEntry(entry);
            if (profile == selected) {
                this.setSelected(entry);
            }
        }
    }

    /**
     * Index of the selected profile.
     *
     * @return the index, or -1 when nothing is selected
     */
    public int selectedIndex() {
        final ProfileEntry entry = this.getSelected();
        return entry == null ? -1 : this.entries.indexOf(entry);
    }

    /** One profile row. */
    public final class ProfileEntry extends Entry<ProfileEntry> {

        /** The profile this row stands for. */
        public final F3Profile profile;

        ProfileEntry(final F3Profile profile) {
            this.profile = profile;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.profile.name);
        }

        @Override
        public void render(final GuiGraphics graphics, final int index, final int top, final int left,
                           final int entryWidth, final int entryHeight, final int mouseX, final int mouseY,
                           final boolean hovered, final float partialTick) {

            final Minecraft minecraft = Minecraft.getInstance();
            final boolean isActive = ProfileManager.activeIndex() == index;

            if (hovered) {
                if (index > 0) {
                    graphics.drawString(minecraft.font, "▲", left + 8, top + 2, 0xFFFFFF, true);
                }
                if (index < ProfileListWidget.this.entries.size() - 1) {
                    graphics.drawString(minecraft.font, "▼", left + 8, top + 14, 0xFFFFFF, true);
                }
            }

            final Component name = isActive
                    ? Component.literal(this.profile.name).withStyle(ChatFormatting.GREEN)
                    : Component.literal(this.profile.name);
            graphics.drawString(minecraft.font, name, left + GUTTER + 4, top + 2, 0xFFFFFF, true);

            final String detail = (isActive ? "active  ·  " : "")
                    + this.profile.left.size() + " left  ·  " + this.profile.right.size() + " right";
            graphics.drawString(minecraft.font, detail, left + GUTTER + 4, top + 14, 0xA0A0A0, true);
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            final int index = ProfileListWidget.this.entries.indexOf(this);
            final double relativeX = mouseX - ProfileListWidget.this.getRowLeft();
            final double relativeY = mouseY - ProfileListWidget.this.getRowTop(index);

            if (relativeX <= GUTTER) {
                if (relativeY < ProfileListWidget.this.itemHeight / 2.0 && index > 0) {
                    ProfileManager.move(index, index - 1);
                    ProfileListWidget.this.parent.reload(index - 1);
                    return true;
                }
                if (relativeY >= ProfileListWidget.this.itemHeight / 2.0
                        && index < ProfileListWidget.this.entries.size() - 1) {
                    ProfileManager.move(index, index + 1);
                    ProfileListWidget.this.parent.reload(index + 1);
                    return true;
                }
            }

            ProfileListWidget.this.setSelected(this);
            ProfileListWidget.this.parent.updateButtons();
            return false;
        }
    }
}
