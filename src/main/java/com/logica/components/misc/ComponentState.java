package com.logica.components.misc;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.components.core.NetComp;
import com.logica.vars.Orientation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ComponentState {
    boolean isOn = false;
    int rotation = 0;
    Map<NetComp, Orientation> activeSources;
    List<Orientation> outputs;
    Vector3i pos;

    public ComponentState(boolean isOn, int rotation, Map<NetComp, Orientation> activeSources,
                          List<Orientation> outputs, Vector3i pos) {
        this.isOn = isOn;
        this.rotation = rotation;
        this.activeSources = activeSources;
        this.outputs = outputs;
        this.pos = pos;
    }

    public ComponentState(boolean isOn, int rotation, Vector3i pos){
        this(isOn, rotation, new HashMap<>(), new ArrayList<>(), pos);
    }

    public void setOn(boolean isOn) {
        this.isOn = isOn;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public void setActiveSources(Map<NetComp, Orientation> activeSources) {
        this.activeSources = activeSources;
    }

    public void setOutputs(List<Orientation> outputs) {
        this.outputs = outputs;
    }

    public void addSource(NetComp source, Orientation inputdir){
        activeSources.put(source, inputdir);
    }

    public void setPos(Vector3i pos) {
        this.pos = pos;
    }

    public void removeSource(NetComp source){
        activeSources.remove(source);
    }

    public void removeSource(Orientation inputdir) {
        activeSources.entrySet().removeIf(entry -> entry.getValue() == inputdir);
    }

    public boolean isOn() {
        return isOn;
    }

    public int rotation() {
        return rotation;
    }

    public Map<NetComp, Orientation> activeSources() {
        return activeSources;
    }

    public List<Orientation> outputs() {
        return outputs;
    }

    public Vector3i pos() {
        return pos;
    }

    public ComponentState withRotation(int rotation) {
        return new ComponentState(isOn, rotation, activeSources, outputs, pos);
    }
}