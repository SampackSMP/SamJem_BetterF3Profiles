package dev.betterf3pp.mixin;

import dev.betterf3pp.profile.ProfileManager;
import me.cominixo.betterf3.config.gui.modules.ModulesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps profile storage in step with BetterF3's own module editor.
 *
 * <p>ModulesScreen.onClose clears BaseModule.modules and refills it from its list widget, then
 * saves BetterF3's config. Because those statics are the active profile, that edit has already
 * landed in the right place in memory; this just makes sure it reaches our config file too.
 */
@Mixin(ModulesScreen.class)
public class ModulesScreenMixin {

    @Inject(method = "onClose", at = @At("TAIL"))
    private void betterf3pp$persistProfile(final CallbackInfo ci) {
        ProfileManager.commitActive();
        ProfileManager.save();
    }
}
