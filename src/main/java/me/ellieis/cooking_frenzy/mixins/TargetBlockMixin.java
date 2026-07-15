package me.ellieis.cooking_frenzy.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.ellieis.cooking_frenzy.events.TargetBlockHit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(TargetBlock.class)
public class TargetBlockMixin {
    @WrapOperation(method = "onProjectileHit", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TargetBlock;updateRedstoneOutput(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/Entity;)I"))
    private int projectileHitMixin(LevelAccessor level, BlockState state, BlockHitResult hitResult, Entity entity, Operation<Integer> original) {
        int signalStrength = original.call(level, state, hitResult, entity);
        try (var invokers = Stimuli.select().forEntity(entity)) {
            invokers.get(TargetBlockHit.EVENT).onTargetHit(hitResult, entity, signalStrength);
        }
        return signalStrength;
    }
}
