# Logica Connection System Plan (Pull Model)

## Goals
- Pull-based propagation (each component pulls input state from neighbors).
- Clear separation: providers, pipes (connectors), gates, consumers (lamps).
- Rotation-aware IO definitions.
- Explicit rules for through-block power and adjacency.

## Tiny Contract (Inputs/Outputs)
- **Inputs:** neighbor component + relative orientation.
- **Output:** a set of active source entries (Map of provider/gate component → direction). No single `isOn` flag needed for pipes.
- **Success:** component state matches live neighbor outputs; propagation respects direction + through-block rules.
- **Error modes:** invalid neighbor type, invalid direction, or disallowed through-block propagation.

## Core Data Model
### `ComponentState`
- Replace/extend state to include:
  - `List<Orientation> outputs` (future-proof for multiple outputs).
  - `Map<NetComp, Orientation> activeSources` (replaces/renames `activatedBy`).
- **Pipe on/off:** derived from `activeSources.isEmpty()`.
- **Source removal:** if a source turns off or disconnects, remove its entry.

## Component Types & Rules
### Providers (Lever, Pressure Plate)
- Implement `updateOutput(world, state)` with no caller required.
- Output to **all 6 faces**.
- Through-block power: **allowed** only on the provider’s own output face.

### Pipes (Connectors)
- Accept power from any face.
- Track multiple sources simultaneously.
- Output to all faces **except** the incoming source direction(s).
- **Never** provide power back to the source component.
- **No** through-block power (pipes can’t power gates/other pipes through a block).

### Gates (AND, NOT, BUFFER, etc.)
- Each gate defines its local-space IO:
  - NOT/BUFFER: 1 input (south), 1 output (north).
  - AND: inputs left/right, output front.
- Use rotation (via `BlockAccessor` + `Orientation`) to resolve actual world directions.
- Through-block power: **only via the output face**.
- The **other 5 faces** of the powered block can still power adjacent components.

### Consumers (Lamps)
- Accept input from **all 6 faces**.
- Never output power.
- Swap model state on power change.

## Through-Block Power Rules
- **Gates/Providers** can power through a solid block only from their output face.
- That solid block can then provide power to adjacent components on its **other 5 faces**.
- **Pipes** cannot power through blocks.
- **Only direct adjacency**; diagonal/indirect is disallowed.

## Pull-Based Update Flow
1. Providers enqueue updates when toggled or stepped on.
2. `LogicaNetworkManager` processes `updateDeque` at 20 TPS.
3. Each component:
   - Pulls neighbor outputs.
   - Updates `activeSources`.
   - If state changed, notifies neighbors.

## Gate Logic Strategy
- `LogicStrategy` calculates output based on current `activeSources` at inputs.
- Gate subclasses define which sides are inputs vs outputs; no hard-coded behavior in shared logic.

## Edge Cases to Handle
1. Multiple sources powering a pipe; only turn off when **all** sources are removed.
2. Source changes rotation; recompute local IO to world directions.
3. Pipe connected to provider + gate simultaneously; ensure no back-power to sources.
4. Solid block receiving strong power from gate output; propagate to its other 5 faces only.
5. Lamp connected to multiple sources; should be on if any source is active.

## Suggested Implementation Steps
1. Update `ComponentState` to use `outputs` + `activeSources`.
2. Add IO methods to `ILogicaComponent` (`getInputs`, `getOutputs`, `canAcceptInputFrom`, `canProvideOutputTo`, `canProvidePowerThroughBlock`).
3. Update `NetComp` to propagate based on `activeSources` instead of a single on/off state.
4. Implement provider output rules (all 6 faces; through-block only from output face).
5. Implement pipe logic (multi-source + no back-power + no through-block).
6. Update each gate subclass to use rotation-aware IO.
7. Ensure `PowerUtil` respects the through-block rules.

## Notes
- `LogicaConstants` and JSON definitions stay authoritative for naming.
- `NetCompHelper` should stay the place for common update utilities.
