# Firestorm Kotlin Migration Plan

This repository now includes an executable import-and-conversion pipeline for `phoenix-firestorm`.

## Implemented in this repository

1. **Repository import** (`RepositoryImport.cloneIfMissing`) that clones upstream source with `--depth 1`.
2. **File inventory** (`RepositoryImport.listFiles`) for deterministic migration accounting.
3. **Kotlin conversion pass** (`KotlinTranspiler.transpileAll`) that walks imported text source files and emits Kotlin artifacts for each input file.
4. **Native stub extraction** (`CLikeKotlinStubGenerator`) that emits Kotlin class and function stubs from C/C++-style source patterns.

## Conversion output contract

For every supported, non-binary file in the imported source tree:

- A Kotlin file is generated under `build/firestorm-migration/kotlin`.
- The file contains:
  - normalized Kotlin `package` based on source path,
  - deterministic object name (`<SourceFileName>Port`),
  - metadata (`sourcePath`, `originalExtension`),
  - extracted native stubs when source is C/C++ (`class` and `fun ... = TODO(...)`),
  - full original source embedded as a Kotlin multiline string (`originalCode`).

This guarantees source preservation while providing immediately compilable Kotlin scaffolding for incremental manual migration.

## Run

```bash
gradle test
```

```bash
gradle run --args='--import-convert'
```
