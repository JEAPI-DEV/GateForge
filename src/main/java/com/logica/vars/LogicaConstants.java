package com.logica.vars;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import java.util.Locale;

/**
 * centralized constants for the GateForge mod.
 */
public class LogicaConstants {
    public static final String MOD_ID = "gateforge";

    /**
     * Enumerates all known Logica block IDs.
     */
    public enum BlockId {
        PIPE("gateforge_pipe"),
        GATE_AND("gateforge_gate_and"),
        GATE_OR("gateforge_gate_or"),
        GATE_NOT("gateforge_gate_not"),
        GATE_NAND("gateforge_gate_nand"),
        GATE_NOR("gateforge_gate_nor"),
        GATE_XOR("gateforge_gate_xor"),
        GATE_BUFFER("gateforge_gate_buffer"),
        COMP_DFF("gateforge_comp_dff"),
        COMP_CLOCK("gateforge_comp_clock"),
        CONSUMER_LAMP("gateforge_lamp"),
        CONSUMER_PISTON("gateforge_piston"),
        CONSUMER_STICKY_PISTON("gateforge_piston_sticky"),
        PROVIDER_LEVER("gateforge_lever"),
        PROVIDER_PRESSURE_PLATE("gateforge_pressure_plate");

        private final String id;

        BlockId(String id) {
            this.id = id.toLowerCase(Locale.ROOT);
        }

        public String id() {
            return id;
        }

        public static BlockId from(String raw) {
            if (raw == null)
                return null;
            String normalized = raw.toLowerCase(Locale.ROOT);
            // Exact match preferred
            for (BlockId value : values()) {
                if (value.id.equals(normalized)) {
                    return value;
                }
            }
            // Fallback: handle state-suffixed ids (e.g., *_state_definitions_active)
            for (BlockId value : values()) {
                if (normalized.contains(value.id)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * Checks if the given block ID belongs to the Logica mod.
     */
    public static boolean isLogicaBlock(String blockId) {
        return BlockId.from(blockId) != null;
    }

    public static boolean isLogicaComponent(BlockType bt) {
        return bt != null && bt.getId() != null && LogicaConstants.isLogicaBlock(bt.getId());
    }
}