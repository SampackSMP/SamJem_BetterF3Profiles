package dev.betterf3pp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NeoForge entry point.
 *
 * <p>As on Fabric, this does nothing but announce itself: setup happens from the tail of
 * BetterF3's config load so it cannot race mod initialisation order.
 */
@Mod(value = "betterf3pp", dist = Dist.CLIENT)
public final class BetterF3PPNeoForge {

    private static final Logger LOGGER = LogManager.getLogger("BetterF3++");

    public BetterF3PPNeoForge() {
        LOGGER.info("[BetterF3++] Loaded; waiting for BetterF3 to finish loading its config.");
    }
}
