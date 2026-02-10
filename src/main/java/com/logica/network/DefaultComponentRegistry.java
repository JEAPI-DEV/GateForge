package com.logica.network;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.consumers.Lamp;
import com.logica.components.consumers.Piston;
import com.logica.components.consumers.StickyPiston;
import com.logica.components.gate.AndGate;
import com.logica.components.gate.BufferGate;
import com.logica.components.gate.NANDGate;
import com.logica.components.gate.NORGate;
import com.logica.components.gate.NotGate;
import com.logica.components.gate.OrGate;
import com.logica.components.gate.XORGate;
import com.logica.components.gate.custom.Clock;
import com.logica.components.gate.custom.DFF;
import com.logica.components.interfaces.ILogicaComponent;
import com.logica.components.pipes.Pipe;
import com.logica.components.providers.Lever;
import com.logica.components.providers.PressurePlate;
import com.logica.vars.LogicaConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import com.logica.vars.LogicaConstants.BlockId;

/**
 * Default registry mapping BlockId -> component factory.
 */
public class DefaultComponentRegistry implements ComponentRegistry {
    private static final HytaleLogger LOG = HytaleLogger.getLogger();
    private final Map<LogicaConstants.BlockId, BiFunction<Vector3i, World, ILogicaComponent>> registry = new ConcurrentHashMap<>();

    public DefaultComponentRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(BlockId.PIPE, (pos, _) -> new Pipe(pos));
        register(BlockId.GATE_AND, (pos, _) -> new AndGate(pos));
        register(BlockId.GATE_OR, (pos, _) -> new OrGate(pos));
        register(BlockId.GATE_NOT, (pos, _) -> new NotGate(pos));
        register(BlockId.GATE_BUFFER, (pos, _) -> new BufferGate(pos));
        register(BlockId.CONSUMER_LAMP, (pos, _) -> new Lamp(pos));
        register(BlockId.PROVIDER_PRESSURE_PLATE, (pos, _) -> new PressurePlate(pos));
        register(BlockId.PROVIDER_LEVER, (pos, _) -> new Lever(pos));
        register(BlockId.GATE_NOR, (pos, _) -> new NORGate(pos));
        register(BlockId.GATE_XOR, (pos, _) -> new XORGate(pos));
        register(BlockId.GATE_NAND, (pos, _) -> new NANDGate(pos));
        register(BlockId.CONSUMER_PISTON, (pos, _) -> new Piston(pos));
        register(BlockId.COMP_DFF, (pos, _) -> new DFF(pos));
        register(BlockId.COMP_CLOCK, (pos, _) -> new Clock(pos));
        register(BlockId.CONSUMER_STICKY_PISTON, (pos, _) -> new StickyPiston(pos));
    }

    @Override
    public void register(BlockId blockId, BiFunction<Vector3i, World, ILogicaComponent> factory) {
        if (blockId != null && factory != null) {
            registry.put(blockId, factory);
        }
    }

    @Override
    public BiFunction<Vector3i, World, ILogicaComponent> resolve(BlockId blockId) {
        return registry.get(blockId);
    }

    @Override
    public ILogicaComponent create(BlockId blockId, Vector3i pos, World world) {
        BiFunction<Vector3i, World, ILogicaComponent> factory = resolve(blockId);
        if (factory == null) {
            LOG.atWarning().log("[Logica][Registry] No factory for blockId %s", blockId);
            return null;
        }
        return factory.apply(pos, world);
    }
}
