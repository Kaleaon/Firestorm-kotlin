# Firestorm Kotlin Migration Plan

This repository now includes an executable import-and-conversion pipeline for `phoenix-firestorm`.

## Implemented in this repository

1. **Repository import** (`RepositoryImport.cloneIfMissing`) that clones upstream source with `--depth 1`.
2. **File inventory** (`RepositoryImport.listFiles`) for deterministic migration accounting.
3. **Kotlin conversion pass** (`KotlinTranspiler.transpileAll`) that walks imported text source files and emits Kotlin artifacts for each input file.

## Conversion output contract

For every supported, non-binary file in the imported source tree:

- A Kotlin file is generated under `build/firestorm-migration/kotlin`.
- The file contains:
  - normalized Kotlin `package` based on source path,
  - deterministic object name (`<SourceFileName>Port`),
  - metadata (`sourcePath`, `originalExtension`),
  - full original source embedded as a Kotlin multiline string (`originalCode`).

This ensures all imported code is represented in properly formatted Kotlin output, enabling deterministic tracking and incremental replacement with hand-written Kotlin implementations.

## Run

```bash
kotlin MainKt --import-convert
```

Or run steps independently:

```bash
kotlin MainKt --import
kotlin MainKt --convert
```
