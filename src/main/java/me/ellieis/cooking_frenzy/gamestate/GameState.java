package me.ellieis.cooking_frenzy.gamestate;

import me.ellieis.cooking_frenzy.gamestate.upgrades.BaseUpgrade;
import me.ellieis.cooking_frenzy.gamestate.upgrades.DebtUpgrade;

import java.util.ArrayList;
import java.util.List;

public record GameState(int money, int customerCount, int crafterCount, int furnaceCount, int tier, int dayCount, GameModifiers currentModifiers, ArrayList<BaseUpgrade> upgrades) {
    public static GameState defaultGameState() {
        return new GameState(60, 0, 1, 1, 1, 1, new GameModifiers(), new ArrayList<>());
    }

    public GameState incrementCrafter() {
        return new GameState(this.money, this.customerCount, this.crafterCount + 1, this.furnaceCount, this.tier, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState incrementFurnace() {
        return new GameState(this.money, this.customerCount, this.crafterCount, this.furnaceCount + 1, this.tier, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState incrementTier() {
        return new GameState(this.money, this.customerCount, this.crafterCount, this.furnaceCount, this.tier + 1, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState incrementCustomerCount() {
        return new GameState(this.money, this.customerCount + 1, this.crafterCount, this.furnaceCount, this.tier, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState decrementCustomerCount() {
        return new GameState(this.money, this.customerCount - 1, this.crafterCount, this.furnaceCount, this.tier, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState incrementMoney(int money) {
        return new GameState(this.money + money, this.customerCount, this.crafterCount, this.furnaceCount, this.tier, this.dayCount, this.currentModifiers, this.upgrades);
    }

    public GameState decrementMoney(int money) {
        return incrementMoney(-money);
    }

    public GameState incrementDayCounter() { return new GameState(this.money, this.customerCount, this.crafterCount, this.furnaceCount, this.tier, this.dayCount + 1, this.currentModifiers, this.upgrades);}
}
