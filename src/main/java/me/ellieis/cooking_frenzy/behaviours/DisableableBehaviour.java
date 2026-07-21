package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.behaviours.malfunctions.MalfunctionType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.ArrayList;
import java.util.List;

public abstract class DisableableBehaviour extends BaseBehaviour {
    protected boolean isDisabled;
    protected ArrayList<MalfunctionType> disableTypes;
    public DisableableBehaviour(GameSpace gameSpace, GameActivity activity, boolean debugMode, List<MalfunctionType> disableTypes) {
        super(gameSpace, activity, debugMode);
        this.disableTypes = new ArrayList<>(disableTypes);
    }

    public void disableBehaviour(MalfunctionType reason) {
        if (this.disableTypes.contains(reason)) {
            this.isDisabled = true;
            this.onDisable(reason);
        }
    }

    public void enableBehaviour(MalfunctionType reason) {
        if (this.disableTypes.contains(reason)) {
            this.isDisabled = false;
            this.onEnable(reason);
        }
    }

    public boolean isDisabledBy(MalfunctionType type) {
        return disableTypes.contains(type);
    }

    abstract void onDisable(MalfunctionType reason);
    abstract void onEnable(MalfunctionType reason);
}
