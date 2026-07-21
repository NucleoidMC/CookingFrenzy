package me.ellieis.cooking_frenzy.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.ellieis.cooking_frenzy.CookingFrenzy;
import me.ellieis.cooking_frenzy.events.TargetBlockHit;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(TargetBlock.class)
public class TargetBlockMixin {
    @WrapOperation(method = "onProjectileHit", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TargetBlock;updateRedstoneOutput(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/Entity;)I"))
    private int projectileHitMixin(LevelAccessor levelAccessor, BlockState state, BlockHitResult hitResult, Entity entity, Operation<Integer> original, @Local(argsOnly = true, name = "level") Level level) {
        int signalStrength = original.call(levelAccessor, state, hitResult, entity);
        try (var invokers = Stimuli.select().forEntity(entity)) {
            invokers.get(TargetBlockHit.EVENT).onTargetHit(hitResult, entity, signalStrength);
        }
        if (!level.isClientSide() && CookingFrenzy.getGameByLevel(level) != null) {
            return 0;
        }
        return signalStrength;
    }
}
