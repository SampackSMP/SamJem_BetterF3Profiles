package dev.betterf3pp.gui;

import dev.betterf3pp.misc.MiscLineRegistry;
import dev.betterf3pp.profile.F3Profile;
import dev.betterf3pp.profile.ProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Per-line visibility for the scraped misc lines of one profile.
 *
 * <p>Takes the profile explicitly rather than acting on the active one, so misc rules can be
 * edited for any profile without switching the HUD over to it first.
 */
public final class MiscLinesScreen extends Screen {

    private static final int BUTTON_ROW_SPACE = 84;

    private final Screen parent;
    private final F3Profile profile;

    private @Nullable MiscLineListWidget list;
    private @Nullable Button hideNewButton;

    public MiscLinesScreen(final Screen parent, final F3Profile profile) {
        super(Component.translatable("config.betterf3pp.title.misc_lines", profile.name));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        super.init();

        this.list = new MiscLineListWidget(Minecraft.getInstance(), this.profile, this.width,
                this.height - 32 - BUTTON_ROW_SPACE, 32, 28);
        this.list.refresh();
        this.addRenderableWidget(this.list);

        final int left = this.width / 2 - 154;
        final int row1 = this.height - BUTTON_ROW_SPACE + 12;
        final int row2 = row1 + 24;
        final int row3 = row2 + 24;

        this.hideNewButton = this.addRenderableWidget(Button.builder(
                        this.hideNewLabel(), button -> {
                            this.profile.misc.hideNewByDefault = !this.profile.misc.hideNewByDefault;
                            button.setMessage(this.hideNewLabel());
                        })
                .bounds(left, row1, 308, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.misc.forget"), button -> {
                            this.profile.misc.clearDecisions();
                            MiscLineRegistry.clear();
                            if (this.list != null) {
                                this.list.refresh();
                            }
                        })
                .bounds(left, row2, 308, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.done"), button -> this.onClose())
                .bounds(left, row3, 308, 20).build());
    }

    private Component hideNewLabel() {
        return Component.translatable("config.betterf3pp.misc.hide_new")
                .append(CommonComponents.SPACE)
                .append(this.profile.misc.hideNewByDefault
                        ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        if (this.list != null && this.list.isEmpty()) {
            // Misc lines only exist while a world is loaded and other mods have contributed, so an
            // empty list is normal rather than an error. Say so instead of showing a blank box.
            graphics.drawCenteredString(this.font,
                    Component.translatable("config.betterf3pp.misc.empty").withStyle(ChatFormatting.GRAY),
                    this.width / 2, this.height / 2 - 20, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        ProfileManager.save();
        Minecraft.getInstance().setScreen(this.parent);
    }
}
