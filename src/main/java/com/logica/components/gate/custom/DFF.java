package com.logica.components.gate.custom;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NetComp;
import com.logica.components.gate.Gate;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.List;

public class DFF extends Gate {

    private boolean lastClockState = false;

    public boolean isLastClockState() {
        return lastClockState;
    }

    public void setLastClockState(boolean lastClockState) {
        this.lastClockState = lastClockState;
    }

    public DFF(Vector3i position) {
        super(position, LogicaConstants.BlockId.COMP_DFF);
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        if (accessor != null) {
            state.setRotation(accessor.getRotationIndex(getPosition()));
        }

        var inputs = calculateLogicInputs(world);
        boolean dataInput = inputs.size() > 0 && inputs.get(0);
        boolean clockInput = inputs.size() > 1 && inputs.get(1);

        boolean risingEdge = !lastClockState && clockInput;
        boolean newState = state.isOn();
        if (risingEdge) {
            newState = dataInput;
        }
        lastClockState = clockInput;

        state.setOn(newState);
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
