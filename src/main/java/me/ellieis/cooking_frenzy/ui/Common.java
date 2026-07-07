package me.ellieis.cooking_frenzy.ui;

public class Common {
    public static float mapRange(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
}
