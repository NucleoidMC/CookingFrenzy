package me.ellieis.cooking_frenzy.gamestate;

public record ModifierType<T>(String id, Class<T> typeClass) {
    public String toString() {
        return id;
    }
}
