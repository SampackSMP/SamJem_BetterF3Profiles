# BetterF3++

An addon for [BetterF3](https://modrinth.com/mod/betterf3) that adds two things to the debug HUD:

1. **Profiles you can tab between.** Each profile is a complete layout — its own left and right
   module lists, module order, and per-module colours. Press **F3 + Tab** in game to cycle
   forward, **F3 + Shift + Tab** to go back. A tab strip on the HUD shows which one is active.
2. **Per-line control over misc information.** BetterF3's Misc Left/Right modules scrape whatever
   vanilla and other mods left in the debug list and render it as one blob. BetterF3++ lets you
   hide individual lines from that blob and remembers the choice.

Requires **Minecraft 1.21.1** and **BetterF3 11.0.x** on **Fabric** or **NeoForge**. Client-side only.

## Using it

| Action | How |
| --- | --- |
| Manage profiles | `F3 + M` (BetterF3's config screen) → **Profiles...** |
| Switch profile | `F3 + Tab`, or `F3 + Shift + Tab` to go backwards |
| Edit a profile's modules | Profiles screen → select → **Edit Left/Right Modules** |
| Hide a misc line | Profiles screen → select → **Misc Lines** → click a row to toggle it |

Editing modules hands off to BetterF3's own editor, which always edits whichever profile is
active — so selecting a profile and pressing **Edit** makes it active first. That is deliberate:
there is one module editor, not two that can disagree.

The **Misc Lines** screen lists lines seen on the HUD this session, plus any line you hid in an
earlier session (otherwise hiding a line would make it impossible to find again). Turning on
*Hide new lines by default* flips the screen into an allowlist: nothing shows until you enable it.

### The tab strip

The strip is a normal BetterF3 module called **Profile Tabs**, so you can move it, recolour it,
put it on the right instead, or delete it entirely using BetterF3's own module editor. Its *name
colour* is the inactive tabs, its *value colour* is the active one. It hides itself when there is
only one profile.

## Config

`config/betterf3pp.json`, written next to BetterF3's own `config/betterf3.json`. BetterF3 keeps
managing its file, which always mirrors the active profile; every profile lives in ours. A corrupt
or hand-broken file is never fatal — the mod falls back to importing whatever BetterF3 has loaded,
which is what a first run does anyway.

The keybind is configurable there (`keys.cycle` is a GLFW key code; `258` is Tab).

## Building

The build needs **JDK 21**. If `java -version` on your PATH is not 21, point `JAVA_HOME` at one:

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew build
```

Output jars land in `fabric/build/libs/` and `neoforge/build/libs/` (take the ones *without* a
`-dev` or `-sources` classifier).

```bash
./gradlew :common:test        # unit tests for the misc-line key derivation
./gradlew :fabric:runClient   # dev client, with BetterF3 already on the classpath
./gradlew :neoforge:runClient
```

Both dev clients pull in Cloth Config as `modRuntimeOnly`. BetterF3++ does not use it — its
screens are vanilla widgets — but BetterF3's NeoForge build marks `cloth_config` mandatory, so
without it the NeoForge client stops at *"Mod betterf3 requires cloth_config 15.0.0 or above"*.
BetterF3's Fabric build does not declare it at all, even though its General Settings and Edit
Module screens use it, so it is present there too in order to exercise the full `F3 + M` flow.

BetterF3 is pulled from the Modrinth maven. Its version numbers are ambiguous there — all three
loaders publish "11.0.3" — so `gradle.properties` pins the per-loader Modrinth *version IDs*
instead.

## How it attaches to BetterF3

BetterF3 has no addon API, so this mod reaches into it. Worth knowing if you ever update the
BetterF3 dependency — these are the five things it relies on, all in `dev.betterf3pp.mixin`:

| What we use | Why it works |
| --- | --- |
| `BaseModule.modules` / `modulesRight` are public, non-final statics | A profile switch is a field assignment; no render mixin needed |
| `DebugLineList.values(List)` | The single choke point both misc modules funnel through, on every loader |
| `ModConfigFile.load` | Injecting at its return is the one moment BetterF3 is guaranteed to be fully initialised, whatever order the loader started mods in |
| `ModulesScreen.onClose` | Upstream's editor writes straight into the statics; this persists that edit to the right profile |
| `ModConfigScreen.init` | Adds the Profiles button to `F3 + M` |

Module state is captured reflectively (`ModuleSnapshot`) rather than by copying BetterF3's
hand-written `instanceof` chain: new upstream module fields are picked up automatically, and it
avoids NightConfig, which BetterF3 relocates into its own package on Fabric but not on NeoForge.

**Known sharp edge:** `BaseModule`'s constructors are public on the 1.21 branch but were made
package-private upstream afterwards, so the tab strip will need reworking to port this past
1.21.1. The dependency is pinned to `[11.0.0, 12.0.0)` for that reason.
