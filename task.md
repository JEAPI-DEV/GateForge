# Logica Connection System Tasks

## Step-by-step Implementation
1. **Inspect current component interfaces**
   - Review `ILogicaComponent`, `NetComp`, and `ComponentState` for current IO/state APIs.
   - Identify any existing directional or power propagation logic to avoid duplication.

2. **Update `ComponentState` data model**
   - Add `List<Orientation> outputs` for future multi-output support.
   - Replace/rename `activatedBy` to `activeSources: Map<NetComp, Orientation>`.
   - Ensure pipes derive on/off state from `activeSources.isEmpty()`.

3. **Expand component IO contract**
   - Add/confirm methods on `ILogicaComponent`:
     - `getInputs(World world)`
     - `getOutputs(World world)`
     - `canAcceptInputFrom(pos, relativeDir)`
     - `canProvideOutputTo(pos, relativeDir)`
     - `canProvidePowerThroughBlock(blockPos)`

4. **Refactor `NetComp` propagation logic**
   - Use `activeSources` instead of a single on/off flag.
   - Update change detection to compare source maps.
   - Notify neighbors only when source set changes.

5. **Implement providers (lever/plate)**
   - Output to all 6 faces.
   - Through-block power only on output face.
   - Trigger `updateOutput` on interaction events (already wired by managers).

6. **Implement pipes (connector rules)**
   - Accept inputs from any face.
   - Track multiple sources concurrently.
   - Output to all faces except the source direction(s).
   - Remove a source when its output is off.
   - No through-block power.

7. **Implement gate IO + logic**
   - Define local IO per gate:
     - NOT/BUFFER: input south, output north.
     - AND: inputs left/right, output front.
   - Rotate local IO via `Orientation` and `BlockAccessor`.
   - Through-block power only via output face.

8. **Implement consumers (lamps)**
   - Accept input from all 6 faces.
   - Never output power.
   - Update model on state change.

9. **Enforce through-block rules in `PowerUtil`**
   - Gates/providers can power through block only on output face.
   - Pipes cannot power through blocks.
   - Only direct adjacency counts.

10. **Update `LogicaNetworkManager` tick flow**
    - Ensure queued updates resolve with pull-based evaluation.
    - Confirm 20 TPS processing and no redundant updates.

11. **Add minimal tests (if test harness exists)**
    - Pipe multi-source on/off behavior.
    - Gate rotation IO mapping.
    - Through-block power rule validation.

12. **Document**
    - Update `plan.md` or add notes on behavior changes.
    - Record any configuration constants used by IO rules.
