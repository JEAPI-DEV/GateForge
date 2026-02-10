# Migration Plan: Legacy Components → New Pull Model

This document describes how to migrate the copied legacy components (gates, pipes, providers) into the new pull-based architecture in this codebase.

## 1) High-Level Differences
### Legacy patterns observed
- Classes still reference old packages (e.g., `com.logica.core.*`, `com.logica.models.*`, `com.logica.api.*`).
- Many types and methods no longer exist (e.g., `PowerSource`, `ILogicaComponent` from old API, `getFacing()`, `position`, `state.withOn`, `state.withActivatedBy`).
- Push-style logic and direct “powered” flags are used (e.g., `isActive()` without per-source tracking).
- Pipes rely on single-source (`activatedBy`) rather than multi-source sets.
- Strong power checks are mixed into component logic instead of centralized in `PowerUtil`.

### New model to target
- Pull-based power: receivers pull from neighbors using `getOutputs()` and `isProvidingPowerTo()`.
- Source tracking via `ComponentState.activeSources: Map<NetComp, Orientation>`.
- Directional IO rules via `canAcceptInputFrom`, `canProvideOutputTo`, and `canProvidePowerThroughBlock`.
- Strong power through blocks handled in `PowerUtil` and *only for gate/provider output faces*.

## 2) Migration Strategy (Step-by-Step)

### Step A — Update imports + base classes
- Replace any legacy imports:
  - `com.logica.core.Gate` → `com.logica.components.gate.Gate`
  - `com.logica.models.Orientation` → `com.logica.vars.Orientation`
  - `com.logica.api.ILogicaComponent` → `com.logica.components.interfaces.ILogicaComponent`
  - `com.logica.core.Connector` → `com.logica.components.core.Connector`
  - `com.logica.system.managers.LogicaNetworkManager` → `com.logica.network.LogicaNetworkManager`
- Ensure all providers extend `com.logica.components.core.PowerProvider`.
- Ensure all pipes extend `com.logica.components.core.Connector`.

### Step B — Gates (And/Or/Nand/Nor/Xor/Not/Buffer)
- Remove references to `getFacing()` from old `Gate` base.
  - Use `Orientation.fromRotation(state.rotation())` inside the new `Gate` base.
  - Call `getInputDirections()` which should already return world-space directions.
- Ensure each gate defines only **input directions**; output is handled by `Gate.getOutputDirection()`.
- Keep logic strategies but ensure they use the new `LogicStrategy` and `List<Boolean>`.

**Example change:**
- Old: `Orientation base = getFacing(); base.getLeft()`
- New: `Orientation base = Orientation.fromRotation(state.rotation()); base.getLeft()`

### Step C — Pipes
- Replace old pipe logic with multi-source pull behavior:
  - Track sources in `state.activeSources`.
  - Output to all faces **except** the incoming source direction(s).
  - Never back-power a source.
- Remove old `activatedBy` or single-source logic.
- Ensure `canProvidePowerThroughBlock()` returns **false** for pipes.

### Step D — Providers (Lever, PressurePlate)
- Replace legacy `PowerSource` with `PowerProvider`.
- Use `updateOutput(world, boolean)` to toggle state.
- Output to all 6 faces.
- Through-block power only from the provider’s output face.

### Step E — Strong Power
- Ensure `PowerUtil.isSolidBlockReceivingStrongPower` is the *only* place handling block-through power.
- Gate/provider `canProvidePowerThroughBlock()` should decide if they can power a block.
- Pipes must return `false` for through-block power.

### Step F — Compile & fix mismatches
- Fix errors caused by removed methods (e.g., `withOn`, `withActivatedBy`, `position` fields).
- Update any direct state mutations to use `ComponentState` helpers.
- Remove any old manager references that no longer exist.

## 3) File-by-File Notes

### Gates
- `AndGate`, `OrGate`, `XORGate`, `NANDGate`, `NORGate`:
  - Replace old `com.logica.core.Gate` import and `Orientation` import.
  - Use `Orientation.fromRotation(state.rotation())` for base.

- `NotGate`, `BufferGate`:
  - Replace `getFacing()` with rotation-based orientation.

### Pipes
- `Pipe.java`:
  - Update imports (`ILogicaComponent`, `LogicaNetworkManager`, `LogicaConstants` etc.).
  - Replace all single-source state with `activeSources` set logic.
  - Ensure pipe outputs exclude incoming directions and do not power through blocks.

### Providers
- `Lever.java`, `PressurePlate.java`:
  - Migrate to `PowerProvider`.
  - Replace old `updateBlockState` usage with `NetCompHelper` or existing block accessor usage.
  - Ensure through-block rule is only on their output face (not all adjacent solids).

## 4) Acceptance Criteria
- All components compile with new package structure.
- Gates only accept their intended input directions.
- Pipes support multiple sources and turn off only when all are removed.
- Through-block power works only for gates/providers from their output face.
- No diagonal power propagation.

## 5) Suggested Work Order
1. Update imports + base classes (gates/providers/pipes).
2. Fix compilation errors (methods/fields removed in new base).
3. Refactor pipe logic to multi-source pull.
4. Update gate rotation logic to new `Orientation`.
5. Update provider toggle/strong power logic.
6. Run compile and fix remaining errors.
