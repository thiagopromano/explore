# Expert Elixir LSP — Plugin Implementer's Guide

This document describes the capabilities and behavior of the **Expert** Language Server for Elixir, aimed at developers building IDE/editor plugins that integrate with it.

## Transport

Expert supports two transport mechanisms:

| Mode  | Command                  | Description                              |
|-------|--------------------------|------------------------------------------|
| Stdio | `expert --stdio`         | Default. Reads/writes LSP JSON-RPC via stdin/stdout. |
| TCP   | `expert --port <port>`   | Listens on `localhost:<port>` for LSP connections.    |

Additional CLI flags: `--help`, `--version`.

---

## Initialization

### `initialize` Request

The client sends `initialize` with `rootUri` and optionally `workspaceFolders`. Expert uses these to discover Elixir projects.

**Project detection rules:**

1. **Mix projects** — folders containing a `mix.exs` file.
2. **Bare Elixir projects** — folders containing `*.ex` or `*.exs` files at their root (no `mix.exs`).

Expert creates a `.expert/` directory in each project root for build artifacts, engine data, and index caches.

### Server Info

```json
{
  "name": "Expert",
  "version": "0.1.0-rc.6"
}
```

### Advertised Server Capabilities

```json
{
  "textDocumentSync": {
    "openClose": true,
    "change": 2,
    "save": true
  },
  "completionProvider": {
    "triggerCharacters": [".", "@", "&", "%", "^", ":", "!", "-", "~"]
  },
  "hoverProvider": true,
  "definitionProvider": true,
  "referencesProvider": true,
  "documentFormattingProvider": true,
  "documentSymbolProvider": true,
  "workspaceSymbolProvider": true,
  "codeActionProvider": {
    "codeActionKinds": [
      "quickfix",
      "refactor",
      "refactor.extract",
      "refactor.inline",
      "refactor.rewrite",
      "source",
      "source.fixAll",
      "source.organizeImports"
    ]
  },
  "codeLensProvider": {},
  "executeCommandProvider": {
    "commands": ["Reindex"]
  },
  "workspace": {
    "workspaceFolders": {
      "supported": true,
      "changeNotifications": true
    }
  }
}
```

> `change: 2` means **incremental** sync (`TextDocumentSyncKind.Incremental`).

### `initialized` Notification

After receiving `initialized`, Expert:

1. Registers a **file watcher** for `**/*.{ex,exs}` and `**/mix.lock`.
2. Starts project initialization for each workspace folder (async).
3. Compiles the engine node per project, builds the code index.
4. May prompt the user (via `window/showMessageRequest`) to run `mix deps.get` if dependency errors are detected.

### Client Capabilities Expert Inspects

Expert adjusts behavior based on these client capabilities:

| Capability Path | Effect |
|---|---|
| `textDocument.completion.completionItem.snippetSupport` | Enables snippet completions (e.g., `do/end` blocks) |
| `textDocument.completion.completionItem.deprecatedSupport` | Marks deprecated items |
| `textDocument.completion.completionItem.tagSupport` | Uses tags for deprecated items |
| `textDocument.documentSymbol.hierarchicalDocumentSymbolSupport` | Returns nested symbol trees |
| `textDocument.codeAction.dynamicRegistration` | Dynamic code action registration |
| `window.workDoneProgress` | Enables progress reporting via `$/progress` |
| `window.showMessage` | Enables log/message notifications |

---

## Supported Requests

### `textDocument/completion`

**Trigger characters:** `.` `@` `&` `%` `^` `:` `!` `-` `~`

Returns completion items for:

- **Modules** — project and dependency modules
- **Functions and macros** — with signature, spec, and documentation
- **Variables** — in-scope variables
- **Structs** — struct names and field completion (triggered by `%`)
- **Module attributes** — e.g., `@moduledoc`, custom attributes
- **Behaviours and protocols**
- **Types and typespecs**
- **Bitstring options**
- **Aliases**
- **`use`/`import`/`require` targets**

**Snippet support:** When the client advertises snippet support, Expert provides `do/end` block snippets and function parameter placeholders.

**Partial results:** May return `is_incomplete: true` to signal more results are available as the user continues typing.

> **VS Code note:** Expert always emits a completion response (even if empty) to ensure subsequent completion requests are triggered correctly.

### `textDocument/hover`

Returns Markdown-formatted hover content with:

| Entity | Content |
|---|---|
| Module | Full `@moduledoc`, type definitions, callbacks |
| Function/Macro | Signature with `@spec`, documentation. Macros prefixed with `(macro)`. Shows all matching arities. |
| Struct | Struct definition with field types from `@type t` |
| Type | Type definition with specs |
| Module attribute | Definition text |
| Delegate calls | Resolves to target function |

- Hover responses include a **range** (not just the cursor point).
- Long signatures (>50 chars) are formatted across two header lines.

### `textDocument/definition`

Returns the location(s) where the symbol under the cursor is defined. Supports modules, functions, macros, types, and module attributes.

Returns `null` if no definition is found.

### `textDocument/references`

Returns all references to the symbol under the cursor across the workspace.

Respects the `context.includeDeclaration` parameter — when `true`, the declaration site is included in results.

### `textDocument/formatting`

Formats the entire document. Returns a `WorkspaceEdit` with the formatted content, or `null` if formatting fails.

Uses the project's formatter configuration (`.formatter.exs`).

### `textDocument/codeAction`

Returns code actions filtered by the `context.only` parameter (if provided). Supports these action kinds:

- `quickfix`
- `refactor`
- `refactor.extract`
- `refactor.inline`
- `refactor.rewrite`
- `source`
- `source.fixAll`
- `source.organizeImports`

Each action includes a title, kind, and workspace edit.

### `textDocument/codeLens`

Returns a single code lens:

- **"Rebuild {project_name}'s code search index"** — displayed on the `def project` line in `mix.exs`.
- Only shown when indexing is **not** currently running.
- Triggers the `Reindex` execute command.

### `textDocument/documentSymbol`

Returns a hierarchical tree of symbols in the document.

| Entity | LSP SymbolKind |
|---|---|
| Module | `Module` |
| Function | `Function` |
| Struct | `Struct` |
| Variable | `Variable` |
| Module attribute | `Constant` |
| Type | `TypeParameter` |
| Spec | `Interface` |
| ExUnit test | `Method` |
| ExUnit describe | `Method` |
| ExUnit setup | `Method` |
| ExUnit setup_all | `Method` |
| Protocol | `Module` |
| File | `File` |

Symbols include `detail`, `range`, and `selectionRange`. Children are nested under their parent symbol.

### `workspace/symbol`

Searches for symbols across all active projects.

- **Minimum query length:** configurable via `workspaceSymbols.minQueryLength` (default: **2** characters). Queries shorter than this return no results.
- Returns symbol name, kind, location with range, and container name.

### `workspace/executeCommand`

| Command | Description |
|---|---|
| `Reindex` | Rebuilds the code search index for all active projects. Shows a `window/showMessage` notification on failure. |

---

## Notifications

### Sent by Expert (server → client)

| Notification | Description |
|---|---|
| `textDocument/publishDiagnostics` | Compilation diagnostics per file. Cleared when errors are resolved. |
| `window/showMessage` | Error messages, user prompts (e.g., dependency fetch). |
| `window/logMessage` | Debug/info logging (disabled for Eglot/Emacs to avoid compatibility issues). |
| `$/progress` | Work-done progress updates (if client supports it). |

### Received by Expert (client → server)

| Notification | Description |
|---|---|
| `initialized` | Client confirms initialization; triggers project startup. |
| `exit` | Server exit. |
| `textDocument/didOpen` | Document opened — stored in memory for analysis. |
| `textDocument/didChange` | Incremental content updates to open documents. |
| `textDocument/didSave` | Document saved — may trigger recompilation. |
| `textDocument/didClose` | Document closed — removed from memory. |
| `workspace/didChangeConfiguration` | Settings updated (see Configuration). |
| `workspace/didChangeWatchedFiles` | Watched files changed on disk. |
| `workspace/didChangeWorkspaceFolders` | Workspace folders added or removed at runtime. |

---

## Diagnostics

Expert publishes diagnostics via `textDocument/publishDiagnostics` as compilation proceeds.

**Source:** `"Elixir"`

**Severity mapping:**

| Elixir Compiler | LSP Severity |
|---|---|
| error | `Error` (1) |
| warning | `Warning` (2) |
| information | `Information` (3) |
| hint | `Hint` (4) |

**Types of diagnostics reported:**

- Parse errors
- Compilation errors (`CompileError`, `Mix.Error`, `FunctionClauseError`)
- Undefined function/module warnings
- Unused variable warnings
- HEEx/EEx template errors

**Behavior:**

- Diagnostics are tracked per URI and build number.
- When a file has no more errors, its diagnostics are cleared (empty list published).
- Duplicate diagnostics on the same line are deduplicated.
- Messages are normalized to remove redundant content.

---

## Configuration

Expert accepts workspace settings via `workspace/didChangeConfiguration` or the `initializationOptions` in the `initialize` request.

```json
{
  "logLevel": "info",
  "workspaceSymbols": {
    "minQueryLength": 2
  },
  "additionalWatchedExtensions": []
}
```

| Setting | Type | Default | Description |
|---|---|---|---|
| `logLevel` | `"error"` \| `"warning"` \| `"info"` \| `"log"` | `"info"` | Controls server-side log verbosity. |
| `workspaceSymbols.minQueryLength` | `number` | `2` | Minimum characters before workspace symbol search executes. |
| `additionalWatchedExtensions` | `string[]` | `[]` | Extra file extensions to watch (e.g., `[".heex"]`). Dynamically registers additional file watchers. |

---

## Project Lifecycle

```
Folder Added
  └─▶ Project created (Forge.Project)
       └─▶ Engine node started (async)
            └─▶ Dependencies compiled, index built
                 └─▶ Diagnostics published
                      └─▶ Project active (serving requests)

Folder Removed
  └─▶ Engine node stopped
       └─▶ Project unregistered

Shutdown
  └─▶ All projects stopped
       └─▶ Server halts
```

**Dependency management:** If the engine detects missing dependencies during startup, Expert prompts the user via `window/showMessageRequest` to run `mix deps.get`. If the user declines, the prompt is not shown again for that project during the session.

---

## File Watchers

Expert registers watchers for these patterns after `initialized`:

- `**/*.ex`
- `**/*.exs`
- `**/mix.lock`

Additional extensions can be added via the `additionalWatchedExtensions` configuration setting.

---

## Tips for Plugin Implementers

1. **Use stdio transport** for most editor integrations — it's simpler and the default.
2. **Advertise snippet support** in client capabilities to get richer completions with `do/end` blocks.
3. **Advertise hierarchical document symbol support** for nested symbol trees instead of flat lists.
4. **Handle `window/showMessageRequest`** — Expert uses this to prompt for `mix deps.get` when dependencies are missing. If your plugin doesn't handle this, users may be stuck with a non-functional server.
5. **Respect `is_incomplete`** on completion responses to continue requesting completions as the user types.
6. **Workspace folders** are fully supported — adding/removing folders at runtime will start/stop project engines dynamically.
7. **The `.expert/` directory** is created in project roots. Consider adding it to `.gitignore` templates.
8. **Eglot/Emacs compatibility** — Expert automatically disables `window/logMessage` for Eglot clients to avoid compatibility issues. If building an Emacs plugin with Eglot, this is handled server-side.
