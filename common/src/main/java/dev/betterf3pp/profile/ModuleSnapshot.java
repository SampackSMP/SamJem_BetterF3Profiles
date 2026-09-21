package dev.betterf3pp.profile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import me.cominixo.betterf3.modules.BaseModule;
import me.cominixo.betterf3.modules.EmptyModule;
import me.cominixo.betterf3.utils.DebugLine;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

/**
 * A serialisable snapshot of one configured {@link BaseModule}.
 *
 * <p>BetterF3 persists its modules with a long hand-written instanceof chain in
 * ModConfigFile. We deliberately do not mirror that chain, for two reasons: it would need
 * updating every time upstream adds a module, and reaching its private loadModule/saveModule
 * helpers is not portable, because their descriptors mention NightConfig, which BetterF3
 * relocates into its own package on Fabric but not on NeoForge.
 *
 * <p>Instead we note that every value BetterF3 stores beyond the shared ones is a public,
 * non-final instance field on the module subclass (colorX, emptyLines, timeFormat, and so on)
 * and capture those reflectively. New upstream fields are picked up for free.
 */
public final class ModuleSnapshot {

    /** BetterF3's module id, for example fps, coords or empty. */
    public String id = "";

    /** Whether the module is drawn at all. */
    public boolean enabled = true;

    /** Per-line enable flags, keyed by DebugLine.id(). */
    public Map<String, Boolean> lines = new LinkedHashMap<>();

    /** Reflectively captured public fields. Colors are stored as packed RGB ints. */
    public Map<String, Object> fields = new LinkedHashMap<>();

    /**
     * Captures the current state of a live module.
     *
     * @param module the module to snapshot
     * @return the snapshot
     */
    public static ModuleSnapshot capture(final BaseModule module) {
        final ModuleSnapshot snapshot = new ModuleSnapshot();
        snapshot.id = module.id;
        snapshot.enabled = module.enabled;

        for (final DebugLine line : module.lines()) {
            snapshot.lines.put(line.id(), line.enabled);
        }

        for (final Field field : module.getClass().getFields()) {
            if (!isPersistable(field)) {
                continue;
            }
            try {
                final Object value = field.get(module);
                if (value == null) {
                    continue;
                }
                snapshot.fields.put(field.getName(),
                        value instanceof TextColor color ? color.getValue() : value);
            } catch (final IllegalAccessException ignored) {
                // Public field on a public class; unreachable in practice.
            }
        }
        return snapshot;
    }

    /**
     * Rebuilds a live module from this snapshot.
     *
     * @return a new module instance, never null
     */
    public BaseModule restore() {
        final BaseModule module = instantiate(this.id);
        module.enabled = this.enabled;

        for (final Map.Entry<String, Boolean> entry : this.lines.entrySet()) {
            final DebugLine line = module.line(entry.getKey());
            if (line != null && entry.getValue() != null) {
                line.enabled = entry.getValue();
            }
        }

        for (final Field field : module.getClass().getFields()) {
            if (!isPersistable(field)) {
                continue;
            }
            final Object stored = this.fields.get(field.getName());
            if (stored == null) {
                continue;
            }
            final Object coerced = coerce(field.getType(), stored);
            if (coerced == null) {
                continue;
            }
            try {
                field.set(module, coerced);
            } catch (final IllegalAccessException | IllegalArgumentException ignored) {
                // Type drifted between versions; keep the module default.
            }
        }
        return module;
    }

    /**
     * Creates a fresh instance of the module with the given id, mirroring how BetterF3's own
     * ModConfigFile.loadModule does it so that failure behaviour matches upstream.
     *
     * @param moduleId the module id
     * @return a new module, falling back to an empty spacer when the id is unknown
     */
    private static BaseModule instantiate(final String moduleId) {
        final BaseModule template = BaseModule.moduleById(moduleId);
        if (template != null) {
            try {
                return template.getClass().getDeclaredConstructor().newInstance();
            } catch (final ReflectiveOperationException | RuntimeException ignored) {
                // EmptyModule has no no-arg constructor; fall through.
            }
        }
        return new EmptyModule(false);
    }

    /**
     * Public fields that look like configuration but are really derived render state.
     *
     * <p>EmptyModule.displayedLines is a dirty flag: its constructor sets it equal to emptyLines,
     * and update() treats "emptyLines != displayedLines" as "the line count changed, sweep all 20
     * lines and deactivate the unused ones". Upstream's own loader restores only emptyLines, so
     * after a load the two differ and that sweep runs. Restoring both would make them equal, the
     * sweep would be skipped, and lines past the configured count would keep the active flag they
     * were constructed with - a frame of extra blank lines after every load and every profile
     * switch.
     *
     * <p>This is the hazard to watch for when adding to the reflective capture: a complete
     * snapshot can defeat an invariant that upstream's deliberately partial one preserves. As of
     * BetterF3 11.0.3 this is the only such field; everything else the capture picks up is exactly
     * what upstream persists.
     */
    private static final Set<String> DERIVED_FIELDS = Set.of("displayedLines");

    /**
     * Whether a field should round-trip through the config.
     *
     * <p>Excludes statics and finals (which covers id and every default constant upstream
     * declares final), the defaultXxx naming convention for the ones that are not final, and
     * enabled, because that is stored explicitly.
     *
     * @param field the candidate field
     * @return true if it should be captured and restored
     */
    private static boolean isPersistable(final Field field) {
        final int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }
        final String name = field.getName();
        if (name.startsWith("default") || "enabled".equals(name) || "lines".equals(name)
                || DERIVED_FIELDS.contains(name)) {
            return false;
        }
        final Class<?> type = field.getType();
        return type == TextColor.class
                || type == int.class || type == Integer.class
                || type == boolean.class || type == Boolean.class
                || type == String.class;
    }

    /**
     * Converts a value read back from JSON into the type the field expects.
     *
     * <p>Gson hands every number back as a Double, so the target type decides.
     *
     * @param type the declared field type
     * @param stored the value from the config
     * @return a value assignable to the field, or null if it cannot be converted
     */
    @Nullable
    private static Object coerce(final Class<?> type, final Object stored) {
        if (type == TextColor.class) {
            return stored instanceof Number number ? TextColor.fromRgb(number.intValue()) : null;
        }
        if (type == int.class || type == Integer.class) {
            return stored instanceof Number number ? number.intValue() : null;
        }
        if (type == boolean.class || type == Boolean.class) {
            return stored instanceof Boolean bool ? bool : null;
        }
        if (type == String.class) {
            return stored instanceof String text ? text : null;
        }
        return null;
    }
}
