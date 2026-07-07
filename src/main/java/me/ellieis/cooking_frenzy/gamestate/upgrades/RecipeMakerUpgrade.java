package me.ellieis.cooking_frenzy.gamestate.upgrades;

import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyPhase;
import me.ellieis.cooking_frenzy.phases.PhaseWithState;
import net.minecraft.network.chat.Component;

public class RecipeMakerUpgrade extends BaseUpgrade {
    RecipeMaker.RecipeMakerType type;
    public RecipeMakerUpgrade(int price, RecipeMaker.RecipeMakerType type) {
        String translationKey = "cooking_frenzy.upgrades." + (type == RecipeMaker.RecipeMakerType.CRAFTER ? "crafter" : "furnace");
        super(Component.translatable(translationKey), Component.translatable(translationKey + ".desc"), price);
        this.type = type;
    }

    public void onBuy(GameState state) {
        // no-op
    }

    @Override
    public void onBuy(PhaseWithState game) {
        if (this.type == RecipeMaker.RecipeMakerType.CRAFTER) {
            game.setState(game.getState().incrementCrafter());
        } else {
            game.setState(game.getState().incrementFurnace());
        }
    }

    @Override
    public void setupEvents() {

    }

    @Override
    public boolean isAvailable(GameState state) {
        if (this.type == RecipeMaker.RecipeMakerType.CRAFTER) {
            return state.crafterCount() < 3;
        } else {
            return state.furnaceCount() < 3;
        }
    }
}
