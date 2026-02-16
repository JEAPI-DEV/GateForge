package com.logica.network;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.components.core.ILogicaComponent;
import com.logica.components.core.NetComp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LogicaPersistenceData implements Resource<EntityStore> {

    public static final BuilderCodec<LogicaPersistenceData> CODEC = BuilderCodec
            .builder(LogicaPersistenceData.class, LogicaPersistenceData::new)
            .append(new KeyedCodec<Map<String, ILogicaComponent>>("Components",
                    new MapCodec<>(LogicaCodec.INSTANCE, HashMap::new), true),
                    (BiConsumer<LogicaPersistenceData, Map<String, ILogicaComponent>>) (data, map) -> data
                            .setComponentsFromStrings(map),
                    (Function<LogicaPersistenceData, Map<String, ILogicaComponent>>) data -> data != null ? data
                            .getComponentsAsStrings() : new HashMap<>())
            .add()
            .build();

    public LogicaPersistenceData() {
        // Clear manager to avoid stale data from previous sessions/worlds
        LogicaNetworkManager.getInstance().setAllComponents(new HashMap<>());
    }

    // Conversion helpers for Codec - delegation to Manager

    private void setComponentsFromStrings(Map<String, ILogicaComponent> stringMap) {
        Map<Vector3i, ILogicaComponent> newMap = new HashMap<>();
        if (stringMap != null) {
            for (Map.Entry<String, ILogicaComponent> entry : stringMap.entrySet()) {
                String[] parts = entry.getKey().split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);

                        ILogicaComponent comp = entry.getValue();
                        if (comp != null) {
                            Vector3i pos = new Vector3i(x, y, z);
                            if (comp instanceof NetComp netComp) {
                                netComp.setPosition(pos);
                            }
                            newMap.put(pos, comp);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        LogicaNetworkManager.getInstance().setAllComponents(newMap);
    }

    private Map<String, ILogicaComponent> getComponentsAsStrings() {
        Map<Vector3i, ILogicaComponent> currentComponents = LogicaNetworkManager.getInstance().getAllComponents();
        Map<String, ILogicaComponent> stringMap = new HashMap<>();
        for (Map.Entry<Vector3i, ILogicaComponent> entry : currentComponents.entrySet()) {
            Vector3i pos = entry.getKey();
            String key = pos.x + "," + pos.y + "," + pos.z;
            stringMap.put(key, entry.getValue());
        }
        return stringMap;
    }

    @Override
    public Resource<EntityStore> clone() {
        // Since we delegate to singleton, clone just returns a new instance.
        // The data is conceptually "the singleton's data".
        return new LogicaPersistenceData();
    }
}
