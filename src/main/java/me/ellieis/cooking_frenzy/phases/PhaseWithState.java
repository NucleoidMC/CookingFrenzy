package me.ellieis.cooking_frenzy.phases;

import me.ellieis.cooking_frenzy.gamestate.GameState;

public interface PhaseWithState {
    GameState getState();
    void setState(GameState state);
}
