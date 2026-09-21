package dev.betterf3pp.modules;

import dev.betterf3pp.profile.F3Profile;
import dev.betterf3pp.profile.ProfileManager;
import java.util.List;
import me.cominixo.betterf3.modules.BaseModule;
import me.cominixo.betterf3.utils.DebugLine;
import me.cominixo.betterf3.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * Draws the profile tab strip, for example {@code  Minimal | [Debug] | Network }.
 *
 * <p>This is a real BetterF3 module rather than a render mixin, which buys a lot for very little:
 * it inherits BetterF3's colour handling, spacing, ordering and reduced-debug rules, the user can
 * drag it, recolour it, move it to the right column or delete it with BetterF3's own editor, and
 * it turns up in the Add Module screen automatically. It also keeps us out of the debug-overlay
 * render path, which differs between Fabric and NeoForge.
 *
 * <p>The active profile is drawn in the module's value colour and bracketed; the others use the
 * name colour. Both are editable through BetterF3's generic per-module colour entries.
 */
public final class TabBarModule extends BaseModule {

    /** Format key resolving to a bare "%s", since we build the whole line ourselves. */
    private static final String FORMAT = "format.betterf3pp.tabbar";

    private static final String SEPARATOR = "|";

    private final DebugLine line;

    public TabBarModule() {
        this.defaultNameColor = TextColor.fromRgb(0xAAAAAA);
        this.defaultValueColor = TextColor.fromRgb(0xFFFF55);
        this.nameColor = this.defaultNameColor;
        this.valueColor = this.defaultValueColor;

        final DebugLine tabLine = new DebugLine("tabbar", FORMAT, true);
        tabLine.name("");
        tabLine.inReducedDebug = true;
        this.line = tabLine;
        this.lines.add(tabLine);
    }

    /**
     * Makes the module known to BetterF3 without placing it on the HUD.
     *
     * <p>Adding a single template to allModules is what lets BaseModule.moduleById resolve
     * "tabbar" when we rebuild profiles from the config, and what lists it in BetterF3's Add
     * Module screen. Instances created afterwards use the plain constructor and stay out of the
     * registry, so it does not grow with the number of profiles.
     */
    public static void register() {
        if (BaseModule.moduleById("tabbar") == null) {
            BaseModule.allModules.add(new TabBarModule());
        }
    }

    @Override
    public void update(final Minecraft client) {
        final List<F3Profile> profiles = ProfileManager.profiles();

        // A tab strip listing one tab is just noise.
        if (profiles.size() < 2) {
            this.line.value("");
            this.line.active = false;
            return;
        }

        final TextColor inactive = this.nameColor != null ? this.nameColor : this.defaultNameColor;
        final TextColor activeColor = this.valueColor != null ? this.valueColor : this.defaultValueColor;
        final int activeIndex = ProfileManager.activeIndex();

        final MutableComponent strip = Component.empty();
        for (int i = 0; i < profiles.size(); i++) {
            if (i > 0) {
                strip.append(Utils.styledText(" " + SEPARATOR + " ", inactive));
            }
            final boolean isActive = i == activeIndex;
            // Bracket the active tab so it stays readable without relying on colour alone.
            final String label = isActive ? "[" + profiles.get(i).name + "]" : profiles.get(i).name;
            strip.append(Utils.styledText(label, isActive ? activeColor : inactive));
        }

        this.line.value(List.of(strip));
    }
}
