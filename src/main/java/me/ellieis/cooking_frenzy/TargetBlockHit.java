package me.ellieis.cooking_frenzy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface TargetBlockHit {
    StimulusEvent<TargetBlockHit> EVENT = StimulusEvent.create(TargetBlockHit.class, ctx -> (hitResult, entity, powerLevel) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onTargetHit(hitResult, entity, powerLevel);
                if (result != EventResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return EventResult.PASS;
    });

    EventResult onTargetHit(BlockHitResult hitResult, Entity entity, int powerLevel);
}
