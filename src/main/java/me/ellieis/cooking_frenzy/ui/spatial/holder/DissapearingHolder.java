package me.ellieis.cooking_frenzy.ui.spatial.holder;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;

public class DissapearingHolder extends ElementHolder {
    int ticksLeft;
    public DissapearingHolder(int ticksToDisappear) {
        this.ticksLeft = ticksToDisappear;
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksLeft--;
        if (this.ticksLeft <= 0) {
            for (VirtualElement element : this.getElements()) {
                this.removeElement(element);
            }
            this.destroy();
        }
    }
}
