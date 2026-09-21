package dev.betterf3pp.profile;

import dev.betterf3pp.config.PPConfig;
import dev.betterf3pp.misc.MiscFilter;
import dev.betterf3pp.misc.MiscSide;
import dev.betterf3pp.modules.TabBarModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.cominixo.betterf3.config.GeneralOptions;
import me.cominixo.betterf3.modules.BaseModule;
import me.cominixo.betterf3.utils.Utils;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the set of profiles and which one is live.
 *
 * <p>The mechanism is deliberately small. BetterF3 keeps the active layout in two public,
 * non-final statics, so a profile is just a saved pair of those lists and switching is an
 * assignment. Everything else here is persistence and bookkeeping.
 *
 * <p>All access happens on the client thread (render loop, screens, key handler), so no
 * synchronisation is needed beyond the registry's own.
 */
public final class ProfileManager {

    private static final List<F3Profile> PROFILES = new ArrayList<>();

    private static PPConfig config = new PPConfig();
    private static int activeIndex;
    private static boolean ready;

    private ProfileManager() {
        // Utility class
    }

    /**
     * Loads profiles and applies the active one.
     *
     * <p>Called from the tail of BetterF3's own config load, which is the one moment we can rely
     * on regardless of loader init order: its module registry is fully populated and it has
     * finished writing the statics we are about to take over.
     */
    public static void bootstrap() {
        if (ready) {
            return;
        }
        ready = true;

        TabBarModule.register();
        config = PPConfig.load();

        if (config.profiles.isEmpty()) {
            PROFILES.add(importLiveLayout());
        } else {
            for (final PPConfig.ProfileData data : config.profiles) {
                PROFILES.add(deserialize(data));
            }
        }

        activeIndex = indexOfId(config.activeProfile);
        apply();
        save();
    }

    /**
     * Adopts whatever BetterF3 currently has loaded as the first profile.
     *
     * <p>Keeps the very same list instances so nothing is rebuilt and nothing is lost on a
     * first run with an existing betterf3.json.
     *
     * @return the imported profile
     */
    private static F3Profile importLiveLayout() {
        final F3Profile profile = new F3Profile("default", "Default");
        profile.left = BaseModule.modules;
        profile.right = BaseModule.modulesRight;
        if (findTabBar(profile) == null) {
            profile.left.add(0, new TabBarModule());
        }
        return profile;
    }

    @Nullable
    private static BaseModule findTabBar(final F3Profile profile) {
        for (final BaseModule module : profile.left) {
            if (module instanceof TabBarModule) {
                return module;
            }
        }
        for (final BaseModule module : profile.right) {
            if (module instanceof TabBarModule) {
                return module;
            }
        }
        return null;
    }

    private static F3Profile deserialize(final PPConfig.ProfileData data) {
        final F3Profile profile = new F3Profile(data.id, data.name);
        for (final ModuleSnapshot snapshot : data.modulesLeft) {
            profile.left.add(snapshot.restore());
        }
        for (final ModuleSnapshot snapshot : data.modulesRight) {
            profile.right.add(snapshot.restore());
        }
        profile.misc.hideNewByDefault = data.misc.hideNewByDefault;
        profile.misc.decisions(MiscSide.LEFT).putAll(data.misc.left);
        profile.misc.decisions(MiscSide.RIGHT).putAll(data.misc.right);
        return profile;
    }

    private static PPConfig.ProfileData serialize(final F3Profile profile) {
        final PPConfig.ProfileData data = new PPConfig.ProfileData();
        data.id = profile.id;
        data.name = profile.name;
        for (final BaseModule module : profile.left) {
            data.modulesLeft.add(ModuleSnapshot.capture(module));
        }
        for (final BaseModule module : profile.right) {
            data.modulesRight.add(ModuleSnapshot.capture(module));
        }
        data.misc.hideNewByDefault = profile.misc.hideNewByDefault;
        data.misc.left.putAll(profile.misc.decisions(MiscSide.LEFT));
        data.misc.right.putAll(profile.misc.decisions(MiscSide.RIGHT));
        return data;
    }

    /**
     * Pulls BetterF3's statics back into the active profile.
     *
     * <p>Necessary because BetterF3's own module editor writes straight into them: ModulesScreen
     * clears and refills the list on close, and ModConfigFile.load reassigns it outright.
     */
    public static void commitActive() {
        final F3Profile profile = active();
        if (profile == null) {
            return;
        }
        profile.left = BaseModule.modules;
        profile.right = BaseModule.modulesRight;
    }

    private static void apply() {
        final F3Profile profile = active();
        if (profile == null) {
            return;
        }
        BaseModule.modules = profile.left;
        BaseModule.modulesRight = profile.right;
        config.activeProfile = profile.id;
    }

    /**
     * Makes a profile active, committing any edits to the outgoing one first.
     *
     * @param index position in the profile list; wrapped into range
     */
    public static void switchTo(final int index) {
        if (PROFILES.isEmpty()) {
            return;
        }
        commitActive();
        activeIndex = Math.floorMod(index, PROFILES.size());
        apply();
        if (GeneralOptions.enableAnimations) {
            // Replay BetterF3's slide-in so a switch reads as a switch.
            Utils.xPos = Utils.START_X_POS;
        }
        save();
    }

    /**
     * Steps forward or backward through the profile list.
     *
     * @param delta how many profiles to move, usually 1 or -1
     */
    public static void cycle(final int delta) {
        switchTo(activeIndex + delta);
    }

    /** Writes every profile to disk, capturing the live state of the active one first. */
    public static void save() {
        if (!ready) {
            return;
        }
        commitActive();
        config.profiles.clear();
        for (final F3Profile profile : PROFILES) {
            config.profiles.add(serialize(profile));
        }
        final F3Profile current = active();
        if (current != null) {
            config.activeProfile = current.id;
        }
        config.save();
    }

    /**
     * Creates a new profile containing only a tab strip, and makes it active.
     *
     * @param name display name
     * @return the new profile
     */
    public static F3Profile create(final String name) {
        final F3Profile profile = new F3Profile(uniqueId(name), name);
        profile.left.add(new TabBarModule());
        PROFILES.add(profile);
        switchTo(PROFILES.size() - 1);
        return profile;
    }

    /**
     * Copies a profile, including its module configuration and misc decisions.
     *
     * @param source the profile to copy
     * @return the copy, inserted directly after the source
     */
    public static F3Profile duplicate(final F3Profile source) {
        if (source == active()) {
            commitActive();
        }
        final String name = source.name + " copy";
        final F3Profile copy = new F3Profile(uniqueId(name), name);
        for (final BaseModule module : source.left) {
            copy.left.add(ModuleSnapshot.capture(module).restore());
        }
        for (final BaseModule module : source.right) {
            copy.right.add(ModuleSnapshot.capture(module).restore());
        }
        copy.misc = source.misc.copy();
        PROFILES.add(PROFILES.indexOf(source) + 1, copy);
        save();
        return copy;
    }

    /**
     * Removes a profile. The last remaining profile cannot be deleted, because BetterF3 always
     * needs a layout to render.
     *
     * @param profile the profile to remove
     * @return true if it was removed
     */
    public static boolean delete(final F3Profile profile) {
        if (PROFILES.size() <= 1 || !PROFILES.contains(profile)) {
            return false;
        }
        final int removedIndex = PROFILES.indexOf(profile);
        final boolean removingActive = removedIndex == activeIndex;
        PROFILES.remove(removedIndex);

        if (removingActive) {
            activeIndex = Math.min(removedIndex, PROFILES.size() - 1);
            apply();
        } else if (removedIndex < activeIndex) {
            activeIndex--;
        }
        save();
        return true;
    }

    /**
     * Moves a profile within the list, which is also its tab order.
     *
     * @param from current index
     * @param to target index
     */
    public static void move(final int from, final int to) {
        if (from < 0 || to < 0 || from >= PROFILES.size() || to >= PROFILES.size() || from == to) {
            return;
        }
        final F3Profile active = active();
        Collections.swap(PROFILES, from, to);
        activeIndex = PROFILES.indexOf(active);
        save();
    }

    /**
     * Renames a profile. The id is left alone so the config keeps pointing at the right entry.
     *
     * @param profile the profile to rename
     * @param name the new display name
     */
    public static void rename(final F3Profile profile, final String name) {
        if (name != null && !name.isBlank()) {
            profile.name = name.trim();
            save();
        }
    }

    private static int indexOfId(@Nullable final String id) {
        for (int i = 0; i < PROFILES.size(); i++) {
            if (PROFILES.get(i).id.equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private static String uniqueId(final String name) {
        final String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        final String seed = base.isEmpty() ? "profile" : base;
        String candidate = seed;
        int suffix = 2;
        while (indexOfIdExact(candidate)) {
            candidate = seed + "-" + suffix++;
        }
        return candidate;
    }

    private static boolean indexOfIdExact(final String id) {
        for (final F3Profile profile : PROFILES) {
            if (profile.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The live profile.
     *
     * @return the active profile, or null before bootstrap
     */
    @Nullable
    public static F3Profile active() {
        if (activeIndex < 0 || activeIndex >= PROFILES.size()) {
            return null;
        }
        return PROFILES.get(activeIndex);
    }

    /**
     * Misc visibility rules for the live profile.
     *
     * @return the active filter, or null before bootstrap
     */
    @Nullable
    public static MiscFilter activeFilter() {
        final F3Profile profile = active();
        return profile != null ? profile.misc : null;
    }

    public static int activeIndex() {
        return activeIndex;
    }

    /**
     * All profiles, in tab order.
     *
     * @return a live, unmodifiable view
     */
    public static List<F3Profile> profiles() {
        return Collections.unmodifiableList(PROFILES);
    }

    /** @return true once bootstrap has run */
    public static boolean isReady() {
        return ready;
    }

    public static int cycleKey() {
        return config.keys.cycle;
    }

    public static boolean backOnShift() {
        return config.keys.backModifierShift;
    }

    /**
     * Exposes stored misc decisions so the config screen can list keys that were hidden in an
     * earlier session and are therefore not in this session's registry.
     *
     * @param side the side to read
     * @return the raw decision map for the active profile, empty when there is none
     */
    public static Map<String, Boolean> storedDecisions(final MiscSide side) {
        final MiscFilter filter = activeFilter();
        return filter != null ? filter.decisions(side) : Collections.emptyMap();
    }
}
