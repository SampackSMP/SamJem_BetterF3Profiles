package dev.betterf3pp.gui;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** A one-field prompt, used for creating and renaming profiles. */
public final class NameEditScreen extends Screen {

    private static final int MAX_NAME_LENGTH = 32;

    private final Screen parent;
    private final String initialValue;
    private final Consumer<String> onConfirm;

    private @Nullable EditBox nameBox;
    private @Nullable Button confirmButton;

    public NameEditScreen(final Screen parent, final Component title, final String initialValue,
                          final Consumer<String> onConfirm) {
        super(title);
        this.parent = parent;
        this.initialValue = initialValue;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        this.nameBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20,
                Component.translatable("config.betterf3pp.profiles.name"));
        this.nameBox.setMaxLength(MAX_NAME_LENGTH);
        this.nameBox.setValue(this.initialValue);
        this.nameBox.setResponder(value -> this.updateConfirmState());
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.confirm"), button -> this.confirm())
                .bounds(this.width / 2 - 100, this.height / 2 + 12, 98, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.cancel"), button -> this.onClose())
                .bounds(this.width / 2 + 2, this.height / 2 + 12, 98, 20).build());

        this.updateConfirmState();
    }

    private void updateConfirmState() {
        if (this.confirmButton != null && this.nameBox != null) {
            this.confirmButton.active = !this.nameBox.getValue().isBlank();
        }
    }

    private void confirm() {
        if (this.nameBox != null && !this.nameBox.getValue().isBlank()) {
            this.onConfirm.accept(this.nameBox.getValue().trim());
        }
        this.onClose();
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // Enter confirms, which is what a single-field prompt should do.
        if (keyCode == 257 || keyCode == 335) {
            this.confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}
