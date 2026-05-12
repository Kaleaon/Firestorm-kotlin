# Kotlin Conversion Audit (May 11, 2026)

This repository is predominantly Kotlin already, but many files are still *mechanically converted skeletons* with TODO placeholders rather than complete platform implementations.

## What I checked

- Presence of legacy Java source files (`*.java`) in the repo.
- Volume and concentration of `TODO(...)` markers inside `src/main/kotlin`.
- High-impact modules that still contain explicit “stub/platform port needed” markers.

## Findings

### 1) No remaining Java source files

A recursive search found **0** `*.java` files, so the language-level conversion to Kotlin is complete.

### 2) Large number of unfinished conversion stubs remain

`TODO(...)` markers remain widespread in Kotlin sources and indicate unimplemented native/platform behavior.

Top hotspots by package subtree:

- `com/firestorm/newview`: 11,331 TODOs
- `com/firestorm/llui`: 549 TODOs
- `com/firestorm/llrender`: 188 TODOs
- `com/firestorm/llimage`: 74 TODOs
- `com/firestorm/llprimitive`: 59 TODOs
- `com/firestorm/llcorehttp`: 26 TODOs
- `com/firestorm/llcommon`: 24 TODOs
- `com/firestorm/llcharacter`: 22 TODOs
- `com/firestorm/llcrashlogger`: 19 TODOs
- `com/firestorm/llplugin`: 18 TODOs

### 3) Most missing work is native/runtime integration, not syntax conversion

Representative areas still stubbed:

- Rendering/GPU hooks in `newview` and `llrender`
- UI event wiring in `llui` / `newview`
- Image codec/native codec paths in `llimage`
- Plugin process/socket/shared-memory integration in `llplugin`
- APR/C++ runtime parity shims in `llcommon`

## Suggested completion order

1. **`com/firestorm/newview`** (dominant blocker by TODO count)
2. **`com/firestorm/llui` + `com/firestorm/llrender`** (UI and draw pipeline correctness)
3. **`com/firestorm/llimage` + `com/firestorm/llplugin`** (codecs and plugin/native bridging)
4. **`com/firestorm/llcommon`** (final APR/runtime parity cleanups)

## Bottom line

- **Kotlin conversion status:** complete at source-language level (no Java files).
- **Functional conversion status:** incomplete; significant runtime behavior is still represented by TODO stubs.
