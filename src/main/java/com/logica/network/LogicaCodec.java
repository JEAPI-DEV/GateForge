package com.logica.network;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.components.core.ILogicaComponent;
import com.logica.components.core.NetComp;
import com.logica.components.consumers.Lamp;
import com.logica.components.consumers.Piston;
import com.logica.components.gate.*;
import com.logica.components.gate.custom.*;
import com.logica.components.pipes.Pipe;
import com.logica.components.providers.Lever;
import com.logica.components.providers.PressurePlate;
import com.logica.vars.LogicaConstants;
import org.bson.*;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;

import com.hypixel.hytale.codec.schema.config.ObjectSchema;

public class LogicaCodec implements Codec<ILogicaComponent> {

    public static final LogicaCodec INSTANCE = new LogicaCodec();

    @Override
    public ILogicaComponent decodeJson(com.hypixel.hytale.codec.util.RawJsonReader reader, ExtraInfo extraInfo)
            throws java.io.IOException {
        try {
            reader.consumeWhiteSpace();
            reader.expect('{');

            String idStr = null;
            Vector3i pos = Vector3i.ZERO; // Default to ZERO, will be injected by PersistenceData
            boolean active = false;
            int rotation = 0;
            String stateStr = null; // For Pipe
            int tickCount = 0;
            boolean lastClockState = false;

            while (true) {
                reader.consumeWhiteSpace();
                if (reader.peek() == '}') {
                    break;
                }
                String key = reader.readString();
                reader.consumeWhiteSpace();
                reader.expect(':');
                reader.consumeWhiteSpace();

                switch (key) {
                    case "id":
                        idStr = reader.readString();
                        break;
                    case "pos":
                        // Optional: Read pos if present (legacy support), but usually ignored now
                        pos = Vector3i.CODEC.decodeJson(reader, extraInfo);
                        break;
                    case "state":
                        stateStr = reader.readString();
                        break;
                    case "active":
                        active = reader.readBooleanValue();
                        break;
                    case "rotation":
                        rotation = reader.readIntValue();
                        break;
                    case "tickCount":
                        tickCount = reader.readIntValue();
                        break;
                    case "lastClockState":
                        lastClockState = reader.readBooleanValue();
                        break;
                    default:
                        // Scan forward? We can't easily skip unknown types with RawJsonReader unless we
                        // guess.
                        // For now assume strictly valid JSON from our own save.
                        // But to be safe, maybe just log warning and break/return null?
                        // Or try to skip structure?
                        // RawJsonReader doesn't look like it has generic 'skipValue'.
                        LogicaLogger.warn("LogicaCodec: Unknown JSON key: " + key);
                        // We will desync if we don't consume the value.
                        // Assuming string for safety if unknown? No, could be obj/arr.
                        // Crash is likely if we hit this.
                        return null;
                }

                reader.consumeWhiteSpace();
                if (reader.peek() == ',') {
                    reader.read();
                }
            }
            reader.read(); // '}'

            if (idStr == null) {
                return null;
            }

            LogicaConstants.BlockId id = LogicaConstants.BlockId.from(idStr);
            if (id == null) {
                LogicaLogger.warn("Unknown BlockId in JSON: " + idStr);
                return null;
            }

            ILogicaComponent comp = createComponent(id, pos);

            if (comp instanceof NetComp netComp) {
                netComp.setActive(active);
                netComp.setRotation(rotation);
            }

            if (comp instanceof Pipe pipe && stateStr != null) {
                pipe.setLastState(stateStr);
            }

            if (comp instanceof Clock clock) {
                clock.setTickCount(tickCount);
            } else if (comp instanceof DFF dff) {
                dff.setLastClockState(lastClockState);
            }
            return comp;

        } catch (Exception e) {
            LogicaLogger.error("LogicaCodec.decodeJson failed for component: " + e.getMessage());
            // Return null to indicate failure for this specific component,
            // preventing the entire file load from failing.
            return null;
        }
    }

    @Override
    public Schema toSchema(SchemaContext context) {
        return new ObjectSchema();
    }

    @Override
    public ILogicaComponent decode(BsonValue bsonValue, ExtraInfo extraInfo) {
        try {
            if (!bsonValue.isDocument())
                return null;
            BsonDocument doc = bsonValue.asDocument();

            if (!doc.containsKey("id")) {
                LogicaLogger.warn("Missing 'id' field in BSON document");
                return null;
            }
            String idStr = doc.getString("id").getValue();
            LogicaConstants.BlockId id = LogicaConstants.BlockId.from(idStr);
            if (id == null) {
                LogicaLogger.warn("Unknown BlockId in BSON: " + idStr);
                return null;
            }

            Vector3i pos = Vector3i.ZERO;
            if (doc.containsKey("pos")) {
                pos = Vector3i.CODEC.decode(doc.get("pos"), extraInfo);
            }
            if (pos == null)
                pos = Vector3i.ZERO;

            ILogicaComponent comp = createComponent(id, pos);

            if (comp instanceof NetComp netComp) {
                if (doc.containsKey("active")) {
                    netComp.setActive(doc.getBoolean("active").getValue());
                }
                if (doc.containsKey("rotation")) {
                    netComp.setRotation(doc.getInt32("rotation").getValue());
                }
            }

            if (comp instanceof Pipe pipe && doc.containsKey("state")) {
                pipe.setLastState(doc.getString("state").getValue());
            }

            if (comp instanceof Clock clock && doc.containsKey("tickCount")) {
                clock.setTickCount(doc.getInt32("tickCount").getValue());
            }
            if (comp instanceof DFF dff && doc.containsKey("lastClockState")) {
                dff.setLastClockState(doc.getBoolean("lastClockState").getValue());
            }

            return comp;
        } catch (Exception e) {
            LogicaLogger.error("LogicaCodec.decode failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public BsonValue encode(ILogicaComponent component, ExtraInfo extraInfo) {
        BsonDocument doc = new BsonDocument();
        doc.put("id", new BsonString(component.getBlockId().id()));
        if (component instanceof Pipe pipe && pipe.getLastState() != null)
            doc.put("state", new BsonString(pipe.getLastState()));

        if (component instanceof NetComp netComp) {
            doc.put("active", new BsonBoolean(netComp.isActive()));
            doc.put("rotation", new BsonInt32(netComp.getRotation()));
        }

        if (component instanceof Clock clock) {
            doc.put("tickCount", new BsonInt32(clock.getTickCount()));
        }
        if (component instanceof DFF dff) {
            doc.put("lastClockState", new BsonBoolean(dff.isLastClockState()));
        }

        return doc;
    }

    private ILogicaComponent createComponent(LogicaConstants.BlockId id, Vector3i pos) {
        return switch (id) {
            case PIPE -> new Pipe(pos);
            case GATE_AND -> new AndGate(pos);
            case GATE_OR -> new OrGate(pos);
            case GATE_NOT -> new NotGate(pos);
            case GATE_NAND -> new NANDGate(pos);
            case GATE_NOR -> new NORGate(pos);
            case GATE_XOR -> new XORGate(pos);
            case GATE_BUFFER -> new BufferGate(pos);
            case COMP_DFF -> new DFF(pos);
            case COMP_CLOCK -> new Clock(pos);
            case CONSUMER_LAMP -> new Lamp(pos);
            case CONSUMER_PISTON,
                    CONSUMER_STICKY_PISTON -> // Treating sticky as normal for now or separate class if needed
                new Piston(pos);
            case PROVIDER_LEVER -> new Lever(pos);
            case PROVIDER_PRESSURE_PLATE -> new PressurePlate(pos);
        };
    }
}
