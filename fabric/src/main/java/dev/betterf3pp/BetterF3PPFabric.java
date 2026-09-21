package dev.betterf3pp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fabric entry point.
 *
 * <p>Intentionally almost empty. All of the mod's setup runs from the tail of BetterF3's own
 * config load (see ModConfigFileMixin), because that is the only point where BetterF3 is
 * guaranteed to have registered its modules, regardless of the order the loader initialises mods
 * in. Doing the work here instead would make correctness depend on load order.
 */
@Environment(EnvType.CLIENT)
public final class BetterF3PPFabric implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("BetterF3++");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BetterF3++] Loaded; waiting for BetterF3 to finish loading its config.");
    }
}
