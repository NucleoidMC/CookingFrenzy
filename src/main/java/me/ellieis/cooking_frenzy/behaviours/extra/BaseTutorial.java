package me.ellieis.cooking_frenzy.behaviours.extra;

import me.ellieis.cooking_frenzy.behaviours.TutorialBehaviour;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public abstract class BaseTutorial {
    TutorialBehaviour tutorial;
    GameActivity activity;
    GameSpace gameSpace;
    Active map;
    Scheduler scheduler;
    CookingFrenzyActive game;
    public BaseTutorial(TutorialBehaviour tutorial, GameActivity activity, GameSpace gameSpace, Active map, Scheduler scheduler, CookingFrenzyActive game) {
        this.tutorial = tutorial;
        this.activity = activity;
        this.gameSpace = gameSpace;
        this.map = map;
        this.scheduler = scheduler;
        this.game = game;
    }
}
