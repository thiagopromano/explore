## Project Overview

JetBrains IDE plugin providing Elixir language support via the [Expert](https://github.com/elixir-lang/expert) Language Server. Targets IntelliJ IDEA 2025.3.3+ (build 253+).

This plugin is a **thin layer** over the LSP server. Always prefer using LSP capabilities (code completion, diagnostics, go-to-definition, hover, formatting, etc.) over implementing equivalent functionality in the plugin itself.

## Build Commands

```bash
./gradlew buildPlugin          # Compile and package the plugin
./gradlew runIde               # Launch sandboxed IDE with plugin installed
./gradlew verifyPlugin         # Validate plugin structure
```

**Important:** Close the sandboxed IDE before running `./gradlew buildPlugin`, otherwise `buildSearchableOptions` will fail with "Only one instance of IDEA can be run at a time". You can work around this with `./gradlew buildPlugin -x buildSearchableOptions`.

## Architecture

The plugin has six subsystems:

1. **LSP integration** (`lsp/`): `ElixirLspServerSupportProvider` triggers server startup when an Elixir file is opened (checks file extension, not file type). `ElixirLspServerDescriptor` launches `expert --stdio` with the project root as working directory. The Expert binary path is configurable via settings (defaults to `expert` on PATH).

2. **TextMate syntax highlighting** (`ElixirTextMateBundleProvider` + `src/main/resources/textmate/`): Expert does not support LSP semantic tokens, so highlighting uses TextMate grammars sourced from the VS Code Expert extension (`expert-lsp/vscode-expert`). The `prepareSandbox` Gradle task copies the `textmate/` directory outside the JAR so `PluginManagerCore.getPlugin().pluginPath` can resolve it at runtime. `ElixirFileType` is registered with `.ex`/`.exs` extensions and `ElixirEditorHighlighterProvider` delegates to TextMate's `TextMateEditorHighlighterProvider` via reflection — this ensures both editor files and documentation popups use the TextMate `TextMateLexerDataStorage` for proper color resolution. `ElixirSyntaxHighlighterFactory` and `ElixirParserDefinition` exist to satisfy the documentation highlighting pipeline (`QuickDocHighlightingHelper` → `HtmlSyntaxInfoUtil`).

3. **Declaration detection & "Go to Declaration or Usages"** (`lsp/`): Two cooperating extension points plus a caching service fix IntelliJ's GTDU action for Elixir. Without them, Cmd+Click on a definition navigates to itself (a no-op) instead of showing usages, because IntelliJ's LSP integration does not use `textDocument/documentSymbol` to identify declarations.

   - `ElixirDocumentSymbolService` — project-level service that fetches and caches document symbols from Expert via `textDocument/documentSymbol`. Symbols are fetched proactively when files are opened or modified. Includes a retry mechanism on startup because Expert takes ~4.5s to initialize and `sendRequestSync` silently returns null during that period.
   - `ElixirImplicitReferenceProvider` — registered with `order="first"` to run before the platform's `LspImplicitReferenceProvider`. For declaration positions, returns a self-reference (resolving to an `ElixirLspSymbol`) that blocks the LSP provider, causing the GTDU flow to fall through to "show usages." For non-declaration positions, returns null so normal LSP navigation proceeds.
   - `ElixirLspSymbolSearchTargetFactory` — `SymbolSearchTargetFactory` that converts `ElixirLspSymbol` into an `ElixirSearchTarget`. Paired with `ElixirUsageSearcher` (registered via the `com.intellij.searcher` EP) which queries `textDocument/references` with `includeDeclaration = false`, so the declaration is excluded from the usages popup.

4. **Icon provider** (`ElixirIconProvider`): Provides the Elixir icon for `.heex` and `.eex` template files in the project tree via the `iconProvider` extension point. (`.ex`/`.exs` files get their icon from `ElixirFileType`.)

5. **Settings** (`settings/`): `ExpertSettings` is a `PersistentStateComponent` storing the Expert binary path. `ExpertConfigurable` provides a Settings UI panel under Languages.

6. **Color settings & custom highlighting** (`highlighting/`): `ElixirColorSettingsPage` adds an "Elixir" page under Editor > Color Scheme with customizable token styles (modules, functions, variables, atoms, etc.). `ElixirHighlightingPassFactory` reads TextMate token scopes from the editor's highlighter and overlays Elixir-specific `TextAttributeKey`s on top, enabling per-token customization independent of other languages.

## Key Constraints

- **`ElixirFileType` claims `.ex`/`.exs` extensions** and `ElixirEditorHighlighterProvider` delegates to TextMate's provider to preserve TextMate-based highlighting. Do NOT register `.heex`/`.eex` on the FileType — those are handled by TextMate's own file type detection. If you remove the `editorHighlighterProvider`, editor highlighting will break because a plain `LexerEditorHighlighter` lacks the `TextMateLexerDataStorage` needed for TextMate color resolution.
- TextMate grammars must live **outside the JAR** at runtime. The `prepareSandbox` task in `build.gradle.kts` handles this. If you add new resource bundles, follow the same pattern.
- The LSP module (`com.intellij.modules.lsp`) is declared as a `<depends>` in `plugin.xml` but does **not** need an explicit Gradle dependency — it's included in the IntelliJ IDEA platform artifact.
- Uses IntelliJ Platform Gradle Plugin 2.13.1 with Gradle 9.4.0 and Kotlin 2.1.20.
- **`UsageSearcher` must be registered as `<searcher>`**, not `<usageSearcher>`. The correct EP is `<searcher forClass="com.intellij.find.usages.api.UsageSearchParameters" implementationClass="..."/>`, matching how the platform registers `LspUsageSearcher` in `intellij.platform.lsp.impl.xml`.
- **TextMate scope-to-key mappings are hardcoded** in `TextMateDefaultColorsProvider` (36 rules, e.g. `entity.name` → `CLASS_NAME`, `variable` → `LOCAL_VARIABLE`). There is **no extension point** to add custom mappings. To apply Elixir-specific `TextAttributeKey`s, use a `TextEditorHighlightingPass` that reads scopes from the editor's `HighlighterIterator` (`tokenType.toString()` returns the TextMate scope) and overlays custom attributes. Do NOT use regex-based token detection — always read from the TextMate highlighter.

## Related Directories

- **Expert LSP source code**: `../expert` — the Elixir Language Server this plugin integrates with. Useful for understanding LSP capabilities, document symbol responses, and server behavior.

## Testing in the Sandbox IDE

Before launching the sandbox IDE, clear the log file. **Each command must be run as its own separate Bash tool call** — do NOT chain them with `&&` or `;`, as `runIde` needs its own process:

```bash
# 1. Clear the log (separate command)
> .intellijPlatform/sandbox/exact-elixir-support/RM-2025.3.3/log/idea.log

# 2. Launch the IDE (separate command, run in background)
./gradlew runIde
```

Plugin logs are at:
```
.intellijPlatform/sandbox/exact-elixir-support/RM-2025.3.3/log/idea.log
```
