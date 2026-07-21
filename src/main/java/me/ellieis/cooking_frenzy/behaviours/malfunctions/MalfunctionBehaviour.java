package me.ellieis.cooking_frenzy.behaviours.malfunctions;

import net.minecraft.network.chat.Component;

public interface MalfunctionBehaviour {
    /**
     * @param val True is enabled, False is disabled
     */
    void toggleMalfunction(boolean val);
    Component getTitle();
    Component getDesc();
}
