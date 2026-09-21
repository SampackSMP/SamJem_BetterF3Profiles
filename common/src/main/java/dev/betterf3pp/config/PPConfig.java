package dev.betterf3pp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.betterf3pp.profile.ModuleSnapshot;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * On-disk shape of BetterF3++'s own config, plus its reader and writer.
 *
 * <p>This is deliberately a separate file from BetterF3's config/betterf3.json. BetterF3 keeps
 * writing whichever profile happens to be active into its own file, which stays correct, while
 * every profile lives here.
 *
 * <p>Gson rather than NightConfig: BetterF3 shades and relocates NightConfig into
 * me.cominixo.betterf3.libs.nightconfig on Fabric, so that package is not a dependency we can
 * rely on across loaders. Gson ships with Minecraft everywhere.
 */
public final class PPConfig {

    private static final Logger LOGGER = LogManager.getLogger("BetterF3++");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Matches BetterF3's own relative-path convention for config files. */
    private static final Path PATH = Paths.get("config", "betterf3pp.json");

    /** Schema version, so a future migration has something to branch on. */
    public int version = 1;

    /** Id of the profile that should be active on load. */
    public String activeProfile = "default";

    public KeyOptions keys = new KeyOptions();

    public List<ProfileData> profiles = new ArrayList<>();

    /** Keybinds, as GLFW key codes, handled inside the vanilla F3 combo handler. */
    public static final class KeyOptions {
        /** Key pressed with F3 to move to the next profile. Defaults to Tab. */
        public int cycle = GLFW.GLFW_KEY_TAB;

        /** Whether holding shift with that key moves backwards instead. */
        public boolean backModifierShift = true;
    }

    /** One stored profile. */
    public static final class ProfileData {
        public String id = "";
        public String name = "";
        public MiscData misc = new MiscData();
        public List<ModuleSnapshot> modulesLeft = new ArrayList<>();
        public List<ModuleSnapshot> modulesRight = new ArrayList<>();
    }

    /** Per-profile misc line visibility. Values are true for shown, false for hidden. */
    public static final class MiscData {
        public boolean hideNewByDefault = false;
        public Map<String, Boolean> left = new LinkedHashMap<>();
        public Map<String, Boolean> right = new LinkedHashMap<>();
    }

    /**
     * Reads the config, returning defaults when it is missing or unreadable.
     *
     * <p>A corrupt file is never fatal: the mod falls back to importing whatever BetterF3 has
     * already loaded, which is the same thing a first run does.
     *
     * @return the parsed config, or a fresh default instance
     */
    public static PPConfig load() {
        if (!Files.isRegularFile(PATH)) {
            return new PPConfig();
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            final PPConfig parsed = GSON.fromJson(reader, PPConfig.class);
            return parsed != null ? parsed.sanitized() : new PPConfig();
        } catch (final IOException | JsonSyntaxException ex) {
            LOGGER.warn("[BetterF3++] Could not read {}, starting from defaults: {}", PATH, ex.toString());
            return new PPConfig();
        }
    }

    /**
     * Writes the config atomically, so a crash mid-write cannot leave a truncated file.
     */
    public void save() {
        try {
            final Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException atomicUnsupported) {
                Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException ex) {
            LOGGER.warn("[BetterF3++] Could not write {}: {}", PATH, ex.toString());
        }
    }

    /**
     * Replaces nulls that a hand-edited or partial JSON file can leave behind.
     *
     * @return this instance
     */
    private PPConfig sanitized() {
        if (this.keys == null) {
            this.keys = new KeyOptions();
        }
        if (this.profiles == null) {
            this.profiles = new ArrayList<>();
        }
        this.profiles.removeIf(profile -> profile == null || profile.id == null || profile.id.isEmpty());
        for (final ProfileData profile : this.profiles) {
            if (profile.name == null || profile.name.isEmpty()) {
                profile.name = profile.id;
            }
            if (profile.misc == null) {
                profile.misc = new MiscData();
            }
            if (profile.misc.left == null) {
                profile.misc.left = new LinkedHashMap<>();
            }
            if (profile.misc.right == null) {
                profile.misc.right = new LinkedHashMap<>();
            }
            if (profile.modulesLeft == null) {
                profile.modulesLeft = new ArrayList<>();
            }
            if (profile.modulesRight == null) {
                profile.modulesRight = new ArrayList<>();
            }
            profile.modulesLeft.removeIf(PPConfig::isBlankSnapshot);
            profile.modulesRight.removeIf(PPConfig::isBlankSnapshot);
            for (final ModuleSnapshot snapshot : profile.modulesLeft) {
                fixSnapshot(snapshot);
            }
            for (final ModuleSnapshot snapshot : profile.modulesRight) {
                fixSnapshot(snapshot);
            }
        }
        return this;
    }

    private static boolean isBlankSnapshot(final ModuleSnapshot snapshot) {
        return snapshot == null || snapshot.id == null || snapshot.id.isEmpty();
    }

    private static void fixSnapshot(final ModuleSnapshot snapshot) {
        if (snapshot.lines == null) {
            snapshot.lines = new LinkedHashMap<>();
        }
        if (snapshot.fields == null) {
            snapshot.fields = new LinkedHashMap<>();
        }
    }
}
