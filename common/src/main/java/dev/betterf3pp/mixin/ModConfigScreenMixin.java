package dev.betterf3pp.mixin;

import dev.betterf3pp.gui.ProfilesScreen;
import me.cominixo.betterf3.config.gui.ModConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a Profiles button to the screen BetterF3 opens on F3 + M.
 *
 * <p>Extending Screen here is the usual mixin trick for reaching protected members of the target's
 * superclass: addRenderableWidget, width and height all come from it. The constructor exists only
 * to satisfy javac and is discarded when the mixin is applied.
 *
 * <p>BetterF3 places its General Settings button at height/4 - 24 and the two module-order buttons
 * at height/4, so height/4 + 24 continues the column without overlapping anything.
 */
@Mixin(ModConfigScreen.class)
public abstract class ModConfigScreenMixin extends Screen {

    protected ModConfigScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void betterf3pp$addProfilesButton(final CallbackInfo ci) {
        final Screen self = (Screen) (Object) this;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.betterf3pp.profiles_button"),
                        button -> Minecraft.getInstance().setScreen(new ProfilesScreen(self)))
                .bounds(this.width / 2 - 130, this.height / 4 + 24, 260, 20)
                .build());
    }
}
