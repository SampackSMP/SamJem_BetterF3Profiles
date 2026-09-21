package dev.betterf3pp.mixin;

import dev.betterf3pp.profile.F3Profile;
import dev.betterf3pp.profile.ProfileManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds F3 + Tab (and F3 + Shift + Tab) to cycle profiles.
 *
 * <p>Same method and injection point BetterF3's own KeyboardMixin uses. The two do not collide:
 * it claims M and F and returns without cancelling for anything else, so both injectors run and
 * only the one whose key matched cancels.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Shadow
    protected abstract void debugFeedbackTranslated(String key, Object... args);

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void betterf3pp$cycleProfiles(final int key, final CallbackInfoReturnable<Boolean> cir) {
        if (!ProfileManager.isReady() || key != ProfileManager.cycleKey()) {
            return;
        }
        if (ProfileManager.profiles().size() < 2) {
            return;
        }

        final boolean backwards = ProfileManager.backOnShift() && Screen.hasShiftDown();
        ProfileManager.cycle(backwards ? -1 : 1);

        // The tab strip already shows this, but it may be disabled or the overlay may be closed.
        final F3Profile active = ProfileManager.active();
        if (active != null) {
            this.debugFeedbackTranslated("debug.betterf3pp.profile.switched", active.name);
        }
        cir.setReturnValue(true);
    }
}
