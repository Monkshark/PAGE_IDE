<div align="center">

<img src="assets/logo_transparent.svg" alt="PAGE" width="96" />

# PAGE

**A multi-language desktop IDE, written from scratch in Kotlin and Compose Multiplatform.**

[![CI](https://github.com/monkshark/page-ide/actions/workflows/ci.yml/badge.svg)](https://github.com/monkshark/page-ide/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-4285F4?logo=jetpackcompose&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-21%20toolchain-007396?logo=openjdk&logoColor=white)
![Status](https://img.shields.io/badge/status-pre--alpha-orange)

[Documentation](https://monkshark.github.io/page-ide/) · [Devlog](https://monkshark.github.io/categories/page-개발기/) · [한국어 README](README_KR.md)

</div>

---

PAGE is an IDE built on the idea that a codebase is four things at once — text, a graph, a
history, and a conversation — and that an editor should hold all four in one workspace. Today
it edits, navigates, and runs code across 24 languages, installs the language servers and
toolchains it needs on its own, and draws the dependency graph of what you are reading.

> **Pre-alpha.** It is used daily to build itself, but it is not ready to be your only editor.

## Features

**Language intelligence**
- LSP routing across **24 languages** — completion, diagnostics, go-to-definition, hover, rename
- Project-wide symbol index — find usages, unused code, and dead imports without waiting on a server
- Code actions with an inline diff, and a review panel for edits that span several files

**Running code**
- Single-file run for **13 languages** — no project setup, no launch configuration
- Incremental build cache: unchanged sources skip compilation entirely
- Built-in terminal (pty4j) with the run output docked beside it

**Toolchains, installed for you**
- Missing a JDK, Node, Python, Go, Rust, .NET, Dart/Flutter, Swift, or Clang? PAGE downloads and
  installs it in-app, with progress in the status bar and the log in the install panel
- Windows gets MinGW-w64 and the MSVC SDK (via xwin) so C/C++ and Swift work out of the box

**The editor itself**
- Split panes, multi-tab, folding, rainbow brackets, scope guides, inlay hints
- Nine Glass themes, each with a syntax palette designed for it
- Every shortcut lives in one action table — searchable from the palette, listed in Settings,
  and rendered as `Ctrl` or `⌘` depending on the platform

**Atlas**
- Import, call, and module graphs rendered from tree-sitter, not from build metadata

<sub>Languages: Kotlin · Java · Python · TypeScript/JavaScript · Go · Rust · C/C++ · Swift ·
Dart/Flutter · Ruby · PHP · C# · Vue · Svelte · Bash · JSON · YAML · HTML · CSS · SQL ·
Markdown · Dockerfile</sub>

## The four pillars

| | |
|---|---|
| **P**air | An AI companion that watches the work and talks about it — observer, chat, agent, tutor |
| **A**tlas | The codebase as a graph: modules, functions, dependencies |
| **G**lass | A dark-first design system — soft motion, focus, something worth opening daily |
| **E**cho | A timeline of the work itself, kept locally |

Atlas ships and Glass is most of the way there. Pair and Echo are scaffolding — the
[overview](https://monkshark.github.io/page-ide/#guides/overview.md) says what each is meant to become,
and what PAGE deliberately will not do.

## Getting started

You need Git and nothing else — Gradle provisions the JDK.

```bash
git clone https://github.com/monkshark/page-ide.git
cd page-ide
./gradlew :page:app:run
```

```bash
./gradlew build                 # every module, with tests
./gradlew :page:runtime:test    # one module
```

<sub>If `JAVA_HOME` points at a JDK that no longer exists, Gradle stops before it starts. Either fix
the variable or pin it: `org.gradle.java.home=…` in `~/.gradle/gradle.properties`.</sub>

## Architecture

16 Gradle modules. Dependencies flow one way only, and `page:core` at the bottom has no external
dependencies at all.

```
page-ide/
├── shared-core/     parsers, graph model, paths            (jvm + wasmJs)
├── docs-viewer/     the documentation site                 (wasmJs)
└── page/
    ├── core/        identity, shared domain types          (no external deps)
    ├── perf/        startup timing, UI freeze watchdog
    ├── ui/          Glass design system, the code editor
    ├── editor/      text buffer, edit history, lexers, indexes
    ├── lsp/         LSP4J client — transport, capabilities
    ├── language/    routing, document sync, completion, diagnostics
    ├── runtime/     toolchain installers, process running, terminal
    ├── workspace/   file tree, file operations, rename refactor, search
    ├── atlas-view/  overview graph model and renderer      (jvm + wasmJs)
    ├── atlas/       tree-sitter code graph, panels
    ├── git/         `git status --porcelain` integration
    ├── echo/        work timeline                          (scaffolding)
    ├── pair/        LLM adapters                           (scaffolding)
    └── app/         window, entry point, assembly
```

Why the boundaries sit where they do: [architecture guide](https://monkshark.github.io/page-ide/#guides/architecture.md).

## Notable work

Problems worth reading about, written up in the devlog:

- **Kotlin LSP cold start, 145s → 40s.** The language server resolved a Gradle classpath per
  submodule; a single root resolution covers all of them.
- **Swift on Windows.** GNU `@LongLink` and PAX tar headers, MSVC SDK linkage, and `Path`/`PATH`
  collisions in child process environments.
- **Scroll at 20fps → 130fps.** Viewport culling, per-line layout caching, and moving syntax and
  fold parsing off the frame path.

## Contributing

- `main` is protected — branch, open a PR, let CI pass, squash merge.
- CI is ubuntu-latest + Temurin 21 + `./gradlew build`. It has to be green.
- Real behavior comes with unit tests; scaffolding does not.

## License

Undecided while the project is pre-alpha.

## Contact

- Bugs and ideas: [GitHub Issues](https://github.com/monkshark/page-ide/issues)
- Devlog (Korean): <https://monkshark.github.io/categories/page-개발기/>
- Email: justinchoo0814@gmail.com
