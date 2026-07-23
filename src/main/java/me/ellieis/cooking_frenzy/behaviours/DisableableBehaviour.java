package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.behaviours.malfunctions.MalfunctionType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.ArrayList;
import java.util.List;

public abstract class DisableableBehaviour extends BaseBehaviour {
    protected boolean isDisabled;
    protected ArrayList<MalfunctionType> disableTypes;
    protected ArrayList<MalfunctionType> malfunctionsAffectingBehaviour = new ArrayList<>();
    public DisableableBehaviour(GameSpace gameSpace, GameActivity activity, boolean debugMode, List<MalfunctionType> disableTypes) {
        super(gameSpace, activity, debugMode);
        this.disableTypes = new ArrayList<>(disableTypes);
    }

    public void disableBehaviour(MalfunctionType reason) {
        if (this.disableTypes.contains(reason)) {
            if (this.malfunctionsAffectingBehaviour.isEmpty()) {
                this.isDisabled = true;
                this.onDisable(reason);
            }
            this.malfunctionsAffectingBehaviour.add(reason);
        }
    }

    public void enableBehaviour(MalfunctionType reason) {
        if (this.disableTypes.contains(reason)) {
            this.malfunctionsAffectingBehaviour.remove(reason);
            if (this.malfunctionsAffectingBehaviour.isEmpty()) {
                this.isDisabled = false;
                this.onEnable(reason);
            }
        }
    }

    /**
     * Not to be confused with {@link #isCurrentlyDisabledBy(MalfunctionType)}, returns whether this behaviour can be affected by the malfunction type
     * @param type The Malfunction Type
     * @return True or False
     */
    public boolean canBeAffectedBy(MalfunctionType type) {
        return this.disableTypes.contains(type);
    }

    /**
     * Not to be confused with {@link #canBeAffectedBy(MalfunctionType)}, returns whether the behaviour is currently being disabled by the malfunction type
     * @param type The Malfunction Type
     * @return True or False
     */
    public boolean isCurrentlyDisabledBy(MalfunctionType type) { return this.malfunctionsAffectingBehaviour.contains(type); }

    abstract void onDisable(MalfunctionType reason);
    abstract void onEnable(MalfunctionType reason);
}
