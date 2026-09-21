package dev.betterf3pp.mixin;

import dev.betterf3pp.misc.MiscLineFilter;
import java.util.List;
import me.cominixo.betterf3.utils.DebugLine;
import me.cominixo.betterf3.utils.DebugLineList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Per-line filtering for BetterF3's misc modules.
 *
 * <p>MiscLeftModule and MiscRightModule each scrape whatever vanilla and other mods left in the
 * debug list, and both end their update by handing the result to DebugLineList.values. That makes
 * this one method the single choke point for every misc line, on every loader, which is why the
 * filter lives here rather than in the two modules.
 *
 * <p>remap = false because values() is BetterF3's own method, not an override of a Minecraft one,
 * so it keeps its name in the production jar on every loader. The signature is (List)V with no
 * Minecraft types in it either, so there is nothing here for remapping to do.
 */
@Mixin(value = DebugLineList.class, remap = false)
public abstract class DebugLineListMixin {

    @ModifyVariable(method = "values", at = @At("HEAD"), argsOnly = true)
    private List<String> betterf3pp$filterMiscLines(final List<String> values) {
        // Cast through Object: id() is declared on the superclass, and going via the real type
        // avoids relying on @Shadow resolving an inherited member.
        final String lineId = ((DebugLine) (Object) this).id();
        return MiscLineFilter.apply(lineId, values);
    }
}
