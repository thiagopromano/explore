# Exact: Elixir Support

Elixir language support for JetBrains IDEs, powered by the [Expert](https://github.com/elixir-lang/expert) Language Server.

## Features

- Code completion
- Go to definition / find usages
- Hover documentation
- Diagnostics and error reporting
- Code formatting
- Syntax highlighting (via TextMate grammars)
- Customizable color scheme for Elixir tokens

## How it works

Exact takes an **LSP-first approach**: instead of reimplementing Elixir parsing and analysis inside the IDE, it delegates to [Expert](https://github.com/elixir-lang/expert) — the official Elixir Language Server maintained by the Elixir core team.

This is a fundamentally different strategy from [intellij-elixir](https://github.com/KronicDeth/intellij-elixir), which builds its own Elixir parser and semantic analysis directly in the plugin. That approach gives deep IDE integration but requires the plugin to independently keep up with every Elixir language change.

By contrast, Exact is a thin layer over Expert. When the Elixir team ships a new language feature, Expert supports it, and Exact picks it up automatically — no plugin update needed. The trade-off is that Exact's feature set is bounded by what LSP and Expert provide, but in practice Expert covers the features that matter most for day-to-day development.

## Requirements

- **Expert** language server binary installed and available on PATH (or configured in Settings)
- **Elixir** 1.15.3+
- **Erlang/OTP** 25.0+
- **IntelliJ-based IDE** 2025.3.3+ (IntelliJ IDEA, RubyMine, etc.)

## Installation

1. Install Expert following the instructions at [elixir-lang/expert](https://github.com/elixir-lang/expert)
2. Install the plugin from the JetBrains Marketplace (search for "Exact: Elixir Support")
3. Open an Elixir project — the language server starts automatically

If `expert` is not on your PATH, configure the binary location in **Settings > Languages > Exact: Elixir Support**.

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
