package me.ellieis.cooking_frenzy.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.ellieis.cooking_frenzy.events.CropGrowthEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(CropBlock.class)
public class CropBlockMixin {
    @Inject(method="growCrops", at = @At(value="TAIL"))
    public void onCropGrowth(Level level, BlockPos pos, BlockState state, CallbackInfo ci, @Local(name = "age") int age) {
        try (var invokers = Stimuli.select().at(level, pos)) {
            invokers.get(CropGrowthEvent.EVENT).onCropGrowth(pos, age);
        }
    }
}
