package me.ellieis.cooking_frenzy.gamestate;

public class TypedModifier<T> {
    private T value;
    public TypedModifier(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    public void set(T newValue) {
        this.value = newValue;
    }
}
