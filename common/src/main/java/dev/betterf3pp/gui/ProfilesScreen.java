package dev.betterf3pp.gui;

import dev.betterf3pp.profile.F3Profile;
import dev.betterf3pp.profile.ProfileManager;
import me.cominixo.betterf3.config.gui.modules.ModulesScreen;
import me.cominixo.betterf3.utils.PositionEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the set of profiles.
 *
 * <p>Notably this screen contains no module editor. BetterF3's ModulesScreen already edits
 * whatever is in BaseModule.modules, and that is by definition the active profile, so the Edit
 * buttons here just make the selected profile active and hand off to it.
 */
public final class ProfilesScreen extends Screen {

    private static final int BUTTON_ROW_SPACE = 108;

    private final @Nullable Screen parent;

    /** Survives the re-init that Screen does on resize and on returning from a sub-screen. */
    private int pendingSelection = -1;

    private @Nullable ProfileListWidget list;
    private @Nullable Button setActiveButton;
    private @Nullable Button duplicateButton;
    private @Nullable Button renameButton;
    private @Nullable Button deleteButton;
    private @Nullable Button miscButton;
    private @Nullable Button editLeftButton;
    private @Nullable Button editRightButton;

    public ProfilesScreen(final @Nullable Screen parent) {
        super(Component.translatable("config.betterf3pp.title.profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.list = new ProfileListWidget(this, Minecraft.getInstance(), this.width,
                this.height - 32 - BUTTON_ROW_SPACE, 32, 28);
        this.list.refresh();
        this.selectRow(this.pendingSelection);
        this.addRenderableWidget(this.list);

        final int left = this.width / 2 - 154;
        final int mid = this.width / 2 - 50;
        final int right = this.width / 2 + 54;

        final int row1 = this.height - BUTTON_ROW_SPACE + 12;
        final int row2 = row1 + 24;
        final int row3 = row2 + 24;
        final int row4 = row3 + 24;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.new"), button -> {
                            Minecraft.getInstance().setScreen(new NameEditScreen(this,
                                    Component.translatable("config.betterf3pp.profiles.new"), "New profile",
                                    name -> {
                                        ProfileManager.create(name);
                                        this.reload(ProfileManager.activeIndex());
                                    }));
                        })
                .bounds(left, row1, 100, 20).build());

        this.duplicateButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.duplicate"), button -> {
                            final F3Profile selected = this.selected();
                            if (selected != null) {
                                ProfileManager.duplicate(selected);
                                this.reload(this.list == null ? 0 : this.list.selectedIndex() + 1);
                            }
                        })
                .bounds(mid, row1, 100, 20).build());

        this.renameButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.rename"), button -> {
                            final F3Profile selected = this.selected();
                            if (selected != null) {
                                final int index = this.list == null ? 0 : this.list.selectedIndex();
                                Minecraft.getInstance().setScreen(new NameEditScreen(this,
                                        Component.translatable("config.betterf3pp.profiles.rename"), selected.name,
                                        name -> {
                                            ProfileManager.rename(selected, name);
                                            this.reload(index);
                                        }));
                            }
                        })
                .bounds(right, row1, 100, 20).build());

        this.setActiveButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.set_active"), button -> {
                            final int index = this.list == null ? -1 : this.list.selectedIndex();
                            if (index >= 0) {
                                ProfileManager.switchTo(index);
                                this.reload(index);
                            }
                        })
                .bounds(left, row2, 100, 20).build());

        this.miscButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.misc_lines"), button -> {
                            final F3Profile selected = this.selected();
                            if (selected != null) {
                                Minecraft.getInstance().setScreen(new MiscLinesScreen(this, selected));
                            }
                        })
                .bounds(mid, row2, 100, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.delete"), button -> {
                            final F3Profile selected = this.selected();
                            if (selected != null && ProfileManager.delete(selected)) {
                                this.reload(ProfileManager.activeIndex());
                            }
                        })
                .bounds(right, row2, 100, 20).build());

        this.editLeftButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.edit_left"),
                        button -> this.editModules(PositionEnum.LEFT))
                .bounds(left, row3, 152, 20).build());

        this.editRightButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles.edit_right"),
                        button -> this.editModules(PositionEnum.RIGHT))
                .bounds(this.width / 2 + 2, row3, 152, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.done"), button -> this.onClose())
                .bounds(left, row4, 308, 20).build());

        this.updateButtons();
    }

    /**
     * Makes the selected profile active, then opens BetterF3's own module editor on it.
     *
     * <p>The switch is required rather than cosmetic: ModulesScreen reads and writes
     * BaseModule.modules directly, so the profile has to be live before it opens.
     *
     * @param side which column to edit
     */
    private void editModules(final PositionEnum side) {
        final int index = this.list == null ? -1 : this.list.selectedIndex();
        if (index < 0) {
            return;
        }
        ProfileManager.switchTo(index);
        Minecraft.getInstance().setScreen(new ModulesScreen(this, side));
    }

    @Nullable
    private F3Profile selected() {
        if (this.list == null || this.list.getSelected() == null) {
            return null;
        }
        return this.list.getSelected().profile;
    }

    /**
     * Rebuilds the list after a change, restoring the selection.
     *
     * @param selectIndex row to select afterwards; out-of-range values clear the selection
     */
    public void reload(final int selectIndex) {
        if (this.list == null) {
            return;
        }
        this.list.refresh();
        this.selectRow(selectIndex);
        this.updateButtons();
    }

    private void selectRow(final int index) {
        if (this.list == null || index < 0 || index >= this.list.children().size()) {
            return;
        }
        this.list.setSelected(this.list.children().get(index));
        this.pendingSelection = index;
    }

    /** Greys out the buttons that need a selection, or a second profile to exist. */
    public void updateButtons() {
        if (this.list != null) {
            this.pendingSelection = this.list.selectedIndex();
        }
        final boolean hasSelection = this.selected() != null;
        final boolean canDelete = hasSelection && ProfileManager.profiles().size() > 1;

        for (final Button button : new Button[]{this.setActiveButton, this.duplicateButton,
                this.renameButton, this.miscButton, this.editLeftButton, this.editRightButton}) {
            if (button != null) {
                button.active = hasSelection;
            }
        }
        if (this.deleteButton != null) {
            this.deleteButton.active = canDelete;
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ProfileManager.save();
        Minecraft.getInstance().setScreen(this.parent);
    }
}
