package me.ellieis.cooking_frenzy.map;

import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;

import java.util.ArrayList;

public interface MapWithRecipeMaker {
    ArrayList<RecipeMaker> getRecipeMakers(RecipeMaker.RecipeMakerType type);
}
