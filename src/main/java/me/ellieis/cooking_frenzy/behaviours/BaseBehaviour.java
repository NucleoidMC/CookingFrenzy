package me.ellieis.cooking_frenzy.behaviours;

import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public abstract class BaseBehaviour {
    protected final GameSpace gameSpace;
    protected final GameActivity activity;
    public final boolean debugMode;
    public BaseBehaviour(GameSpace gameSpace, GameActivity activity, boolean debugMode) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.debugMode = debugMode;
        this.setupEvents();
    }

    protected abstract void setupEvents();
}
