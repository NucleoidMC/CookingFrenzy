package me.ellieis.cooking_frenzy.behaviours;

import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;

// future behaviour idea where a behaviour could become faulty and get disabled randomly
// then the player needs to fix it somehow
public abstract class DisableableBehaviour extends BaseBehaviour {
    private boolean isDisabled;
    public DisableableBehaviour(GameSpace gameSpace, GameActivity activity, boolean debugMode) {
        super(gameSpace, activity, debugMode);
    }

    public void disableBehaviour() {
        this.isDisabled = true;
    }

    public void enableBehaviour() {
        this.isDisabled = false;
    }

    abstract void onDisable();
}
