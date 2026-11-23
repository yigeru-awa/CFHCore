# CFHCore Development Standards

Version 1.0 — normative for this repository. When a rule here conflicts with a personal
habit, the rule here wins. If a rule needs to change, change this file in the same
commit as the code that relies on it.

Scope: every file tracked in this repository, except the two explicit exceptions named
in §2.2.

---

## 1. Language policy (hard rules)

### 1.1 English only

The following must be written in English, with no Chinese characters anywhere:

- Java/Kotlin source code, identifiers, comments, Javadoc
- Log messages, exception messages, user-facing tooltips written in code
- Config keys, registry IDs, resource IDs, data pack JSON field names
- File and directory names
- Gradle scripts, properties, TOML metadata
- Commit messages, branch names, issue titles

### 1.2 The only exception: Chinese localization

Chinese may appear **only** in translation files:

```
src/main/resources/assets/<namespace>/lang/zh_cn.json
```

Rules for those files:

- `zh_cn.json` must contain the same key set as `en_us.json` (no extra, no missing).
- `en_us.json` is the source of truth. `zh_cn.json` translates; it never introduces new
  keys that do not exist in `en_us.json`.
- Keys themselves stay ASCII (`cfhcore.gui.button.confirm`), even in `zh_cn.json`.
- Full-width Chinese punctuation (，。！？：) is allowed inside `zh_cn.json` values only.

### 1.3 No decorative Unicode in code

No emoji, no box-drawing characters, no non-ASCII identifiers, no smart quotes in code.
English comments only — a Chinese comment is a review blocker, not a nit.

---

## 2. Source layout

```
src/main/java/com/onehumanawa/cfhcore/
  CFHCore.java                 # mod entry point (@Mod), global wiring only
  CFHAllBlocks.java            # block registry aggregates
  CFHAllBlockEntityTypes.java
  CFHAllContentRegistries.java
  content/<feature>/...        # gameplay features, one root package per feature
  foundation/<mechanism>/...   # cross-feature mechanisms (ctm, media, ...)
src/main/resources/
  assets/<namespace>/...       # textures, models, lang, shaders, scenes
  data/<namespace>/...         # data pack content
  META-INF/...                 # accesstransformer.cfg
src/main/templates/META-INF/neoforge.mods.toml   # expanded at build time
src/generated/resources/       # runData output — git-ignored, never hand-edited
```

- Package name = feature name, lowercase, no underscores, no abbreviations.
  `content.worldgen`, `content.gui`, `foundation.ctm`.
- Sub-packages are allowed when a feature has more than ~10 classes or a clear
  sub-domain (`content.gui.core.stage.mesh`, `content.worldgen.cell.biome`).
- One public top-level class per file; file name == class name.
- Client-only code goes under a `client` sub-package (or is guarded by
  `@OnlyIn(Dist.CLIENT)` / `Dist.CLIENT` checks) so the dedicated server never loads it.
- Feature classes that would collide with Minecraft, NeoForge, Create or a ported
  upstream class are prefixed `CFH` (`CFHGuiButton`, `CFHGuiScrollPanel`).

### 2.1 Namespaces

| Namespace | Used for |
|---|---|
| `cfhcore` | game content: blocks, items, worldgen data, worldgen structures |
| `cfhgui` | GUI assets ported from the Slate series (textures, fonts, shaders, scenes) |

---

## 3. Naming

| Element | Convention | Example |
|---|---|---|
| Class / record / enum | PascalCase | `CFHGuiScrollPanel` |
| Method / field / parameter | camelCase | `scrollTo`, `lastFrameTime` |
| Constant / `static final` | UPPER_SNAKE_CASE | `MAX_RETRIES` |
| Boolean | reads as `is` / `has` / `can` / `should` | `isVisible`, `hasTexture` |
| Package | lowercase, dot separated | `content.gui.core.widget` |
| Resource ID / registry name | snake_case | `cfhcore:burner` |
| Mixin accessor | `<Target>Accessor` | `ScreenRenderablesAccessor` |
| Mixin invoker | `<Target>Invoker` | `OptionsScreenInvoker` |

---

## 4. Formatting

- Indentation: **tabs** (the existing sources use tabs; do not introduce spaces).
- Braces: K&R — opening brace on the same line as the declaration.
- Line length: keep under 120 columns; 160 is the hard ceiling.
- One statement per line. No wildcard imports (`import java.util.*;` is forbidden).
- Import order (blank line between groups):
  1. `java.*`, `javax.*`
  2. third-party (`org.*`, `com.google.*`, `it.unimi.*`, `org.slf4j.*`, `org.apache.*`)
  3. `com.mojang.*`
  4. `net.minecraft.*`
  5. `net.neoforged.*`, `cpw.mods.*`
  6. `com.onehumanawa.cfhcore.*`
  7. everything else
- Prefer `var` only for obvious locals; do not use it for fields or return values.
- Never leave commented-out code in a commit. Delete it or turn it into a Javadoc note.
- `TODO` markers must carry an owner and a reason: `// TODO(OneHumanAwa): needs a 1.21.4 API check`.
  A bare `// TODO` is not allowed.

---

## 5. Comments and Javadoc

- English only. Explain **why**, not **what** — the code already says what.
- Public API and non-obvious classes get Javadoc: purpose, invariants, threading notes.
  `@param` / `@return` only when the name does not already say it.
- Block comments (`//`) for explanations inside method bodies; Javadoc (`/** */`) for
  declarations.
- No Chinese in comments — see §1.1.

---

## 6. Localization

- GUI/feature strings live in `assets/<namespace>/lang/en_us.json` and `zh_cn.json`.
- Key layout: `<namespace>.<feature>.<element>`, e.g. `cfhcore.gui.button.confirm`.
- Every new key is added to **both** files in the same commit.
- Tooltips and long text use `\n` line breaks; never rely on auto-wrapping.
- Format placeholders use `%s` / `%d` in the English source; the Chinese translation must
  keep the same placeholders and the same order.

---

## 7. Resources

- Textures: `assets/<ns>/textures/<area>/<name>.png`, snake_case.
- Models: `assets/<ns>/models/<area>/<name>.json`; blockstates in `blockstates/`.
- Mixin configs: `src/main/resources/<mod_id>.<area>.mixins.json`
  (e.g. `cfhcore.gui.menu.mixins.json`). **Every new mixin config must be registered in
  `src/main/templates/META-INF/neoforge.mods.toml`** under `[[mixins]]`, or the client
  will not load the mixins.
- Data pack content: `data/cfhcore/...`. Hand-written data stays in
  `src/main/resources/data`; generated data goes to `src/generated/resources` (git-ignored)
  and is produced by `runData`.
- `neoforge.mods.toml` is a template: `${...}` placeholders are expanded by Gradle.
  Because the expansion uses Groovy templating, **never write a literal `${...}` in any
  resource file** — it breaks the build.

---

## 8. Ported third-party code (Slate series)

The Slate modules (`slate-core`, `slate-config`, `slate-menu`, `slate-hud`, and the
remaining `slate-*` modules) are MIT-licensed and are vendored as source, not as jar
dependencies. `LICENSE.md` already records the third-party attribution — keep it
accurate when adding a module.

### 8.1 Package mapping

| Upstream package | CFHCore package |
|---|---|
| `dev.fallingcloud.slate.core` | `com.onehumanawa.cfhcore.content.gui.core` |
| `dev.fallingcloud.slate.config` | `com.onehumanawa.cfhcore.content.gui.config` |
| `dev.fallingcloud.slate.menu` | `com.onehumanawa.cfhcore.content.gui.menu` |
| `dev.fallingcloud.slate.hud` | `com.onehumanawa.cfhcore.content.gui.menu.client.infohud` |
| `dev.fallingcloud.slate.<module>` | `com.onehumanawa.cfhcore.content.gui.<module>` |

### 8.2 Porting rules

- Keep upstream structure, class names, method names and behavior. The port must stay
  diffable against the decompiled upstream source.
- Adaptations are marked with a short `// CFH: <reason>` comment on the changed line or
  directly above it. No other commentary about the porting process in the code.
- Clean decompiler artifacts before committing: synthetic names (`class_1234`,
  `lambda$foo$0`, `/* $FF: synthetic */`), anonymous-class numbering, and bogus
  `var10000` locals. Rename them to meaningful names.
- Do not reformat ported code wholesale (no wholesale re-indent, no line rewrapping).
  It destroys the diff against upstream.
- Optional third-party mods (EMI, JEI, JourneyMap, VoiceChat, DiagonalBlocks, …) that are
  **not** dependencies of CFHCore must never be referenced by a compile-time import.
  Use the compat pattern: `compileOnly` dependency + runtime detection
  (`ModList.get().isLoaded(...)`, `Class.forName(...)`) inside a dedicated
  `compat/<mod>/` package. When the optional mod is absent, the feature degrades to
  "disabled", never to a crash.
- Upstream license headers (`LICENSE_<module>`) stay with the code they cover.

---

## 9. Mixins

- Mixin classes live in a `mixin` sub-package of the feature they patch.
- Every mixin class is listed in a mixin config JSON, and every config JSON is listed in
  `neoforge.mods.toml`.
- Injector methods carry Javadoc naming the target method and the injection semantics
  (`at HEAD`, `at TAIL`, shift, cancellable).
- Accessors/invokers are named per §3 and are the only sanctioned way to reach private
  members. Adding an access transformer requires a matching entry in
  `src/main/resources/META-INF/accesstransformer.cfg`.
- Prefer a public API change over a mixin when one is available.

---

## 10. Networking, registries and lifecycle

- One central registration site per kind: blocks/items in `CFHAllBlocks` /
  `CFHAllContentRegistries`, payloads in the `net` package, mixin configs in
  `neoforge.mods.toml`.
- `mod_id`, the `@Mod` annotation and `neoforge.mods.toml` `modId` must always agree.
- `minecraft_version` and `neo_version` in `gradle.properties` must match the same
  Minecraft release.
- Client/server separation: client-only classes are never touched from common code
  paths; use `Dist.CLIENT` guards or `@OnlyIn(Dist.CLIENT)`.

---

## 11. Temporary and scratch files

- Everything temporary — analysis scripts, decompiler output, logs, scratch copies —
  lives in `.temp/`. It is git-ignored and must never be committed.
- Nothing temporary may live under `src/`, `libs/`, or the repository root.
- Decompiled upstream sources stay in `.temp/decompiled/`; the ported result goes to
  `src/main/java/...`.

---

## 12. Build and verification

- Builds run through the project's Gradle wrapper **via the `mc_gradle` tool**.
  Never invoke `gradlew`/`gradlew.bat` directly from a shell, and never hand-edit
  `gradle-wrapper.properties` to chase a download error.
- Java toolchain: **21** (Minecraft 1.21.1 / NeoForge 21.1.x). The toolchain is
  provisioned by Gradle (foojay); do not install or repoint JDKs by hand.
- A change is finished only when `mc_gradle build` succeeds. A failed compile is not
  "almost done" — it is not done.
- New compiler warnings are not acceptable: fix them or justify them in the commit
  message.
- `mc_gradle runClient` / `runServer` / `runData` are the only ways to exercise the mod.

---

## 13. Git and commit messages

### 13.1 Branches

```
feature/<short-kebab-description>
fix/<short-kebab-description>
chore/<short-kebab-description>
```

### 13.2 Commit messages — Conventional Commits, English, imperative mood

```
<type>(<scope>): <short summary>
```

- Subject line: imperative mood ("add", not "added"/"adds"), no trailing period,
  ≤ 72 characters.
- Allowed types: `feat`, `fix`, `refactor`, `perf`, `docs`, `build`, `test`, `chore`,
  `revert`.
- Allowed scopes: `gui`, `config`, `hud`, `menu`, `worldgen`, `ctm`, `foundation`,
  `content`, `build`, `deps`, `assets`.
- Body: blank line after the subject, explains **why**, wrapped at 100 columns, bullets
  prefixed with `-`.
- One logical change per commit. No `WIP`, no "fix stuff", no "asdf", no Chinese.
- Ported modules use a dedicated scope, e.g.
  `feat(gui): port slate-teams module from upstream MIT sources`.
- Reference issues as `(#123)` at the end of the subject or body.

### 13.3 Never commit

`.temp/`, `libs/`, `build/`, `run/`, `.gradle/`, IDE settings, `src/generated/`,
compiled jars, downloaded toolchains, or any file under `libs/` that is not explicitly
tracked.

---

## 14. Definition of done

A change is done when all of the following hold:

1. `mc_gradle build` passes with no new warnings.
2. No Chinese outside `zh_cn.json` (§1).
3. New user-visible strings exist in both `en_us.json` and `zh_cn.json` with matching
   keys.
4. New mixin configs are registered in `neoforge.mods.toml`.
5. Optional third-party integrations degrade gracefully when the mod is absent.
6. Code follows §3–§5 naming and formatting.
7. The commit message follows §13.2 and the diff contains nothing unrelated.
8. Temporary artifacts stayed in `.temp/`.
