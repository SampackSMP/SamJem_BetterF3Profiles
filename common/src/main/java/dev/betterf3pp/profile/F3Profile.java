package dev.betterf3pp.profile;

import dev.betterf3pp.misc.MiscFilter;
import java.util.ArrayList;
import java.util.List;
import me.cominixo.betterf3.modules.BaseModule;

/**
 * One named debug-HUD layout.
 *
 * <p>The two lists are exactly what BetterF3 keeps in its BaseModule.modules and
 * BaseModule.modulesRight statics; switching profiles is assigning them across.
 */
public final class F3Profile {

    /** Stable slug, used to identify the active profile in the config file. */
    public String id;

    /** Display name, shown in the tab strip and the profiles screen. */
    public String name;

    /** Left-column modules, in render order. */
    public List<BaseModule> left = new ArrayList<>();

    /** Right-column modules, in render order. */
    public List<BaseModule> right = new ArrayList<>();

    /** Which scraped misc lines this profile hides. */
    public MiscFilter misc = new MiscFilter();

    public F3Profile(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
