package me.ellieis.cooking_frenzy.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface CropGrowthEvent {
    StimulusEvent<CropGrowthEvent> EVENT = StimulusEvent.create(CropGrowthEvent.class, ctx -> (BlockPos pos, int age) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onCropGrowth(pos, age);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onCropGrowth(BlockPos pos, int age);
}
