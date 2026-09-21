package dev.betterf3pp.mixin;

import dev.betterf3pp.modules.TabBarModule;
import dev.betterf3pp.profile.ProfileManager;
import me.cominixo.betterf3.config.ModConfigFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts BetterF3++ at the one moment that is safe on every loader.
 *
 * <p>Hooking BetterF3's config load rather than our own mod initialiser removes any dependency on
 * Fabric or NeoForge init ordering: by the time this returns, BetterF3 has registered all of its
 * modules (so BaseModule.moduleById can resolve the ids in our config) and has finished writing
 * the two statics we are about to take over.
 *
 * <p>RETURN rather than TAIL on purpose: load() returns early when config/betterf3.json does not
 * exist, and TAIL would only fire on the last return, so a fresh install would never start us.
 * ProfileManager.bootstrap is idempotent.
 *
 * <p>remap = false: load() is BetterF3's own static method, so it is not renamed in the
 * production jar and must not be looked up in Minecraft's mappings.
 */
@Mixin(value = ModConfigFile.class, remap = false)
public class ModConfigFileMixin {

    /**
     * Makes the tab-strip module resolvable before BetterF3 parses its own config.
     *
     * <p>Once a profile containing the tab strip has been saved, BetterF3's betterf3.json also
     * lists a module named "tabbar", because its save routine serialises whatever is in the
     * statics. Its loadModule then calls BaseModule.moduleById("tabbar"); if we have not
     * registered by that point it quietly substitutes an empty spacer. Nothing breaks, since we
     * replace those lists moments later, but it leaves a stray module behind on every launch.
     */
    @Inject(method = "load", at = @At("HEAD"))
    private static void betterf3pp$registerModules(final ModConfigFile.FileType filetype, final CallbackInfo ci) {
        TabBarModule.register();
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void betterf3pp$bootstrap(final ModConfigFile.FileType filetype, final CallbackInfo ci) {
        ProfileManager.bootstrap();
    }
}
