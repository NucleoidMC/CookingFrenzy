package me.ellieis.cooking_frenzy.gamestate;

import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.events.FoodCookedEvent;
import me.ellieis.cooking_frenzy.mixins.AbstractFurnaceBlockEntityAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.stimuli.Stimuli;

public class Furnace extends RecipeMaker {
    public Furnace(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, FrontAndTop orientation, int timer, float timerMultiplier, boolean debugMode) {
        super(isUnlocked, isMain, isMaking, position, orientation, Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, orientation.front()), timer, timerMultiplier, debugMode);
    }

    public void onMake(ServerLevel level) {
        level.getBlockEntity(this.position, BlockEntityTypes.FURNACE).ifPresent(furnaceBe -> {
            SingleRecipeInput input = new SingleRecipeInput(furnaceBe.getItem(0));
            ((AbstractFurnaceBlockEntityAccessor) furnaceBe).cooking_frenzy$getQuickCheck().getRecipeFor(input, level).ifPresent(recipeHolder -> {
                ItemStack recipe = recipeHolder.value().assemble(input);
                if (!recipe.isEmpty()) {
                    Direction direction = this.orientation.front();
                    DefaultDispenseItemBehavior.spawnItem(level, recipe, 6, direction, Vec3.atCenterOf(this.position).relative(direction, 0.7));
                    furnaceBe.getItem(0).shrink(1);
                    level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, false).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
                    level.setBlock(this.position, Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, orientation.front()).setValue(BlockStateProperties.LIT, false), 2);
                    level.playSound(null, this.position, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1, 1.8f);
                    level.playSound(null, this.position, SoundEvent.createVariableRangeEvent(CustomSounds.OVEN_DING), SoundSource.BLOCKS, 1, 1);
                    Stimuli.select().at(level, this.position).get(FoodCookedEvent.EVENT).onFoodCooked(recipe);
                }
            });
        });
    }

    public void internalLoop(ServerLevel level) {
        if (this.timer % 20 == 0) {
            level.playSound(null, this.position, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1, 1);
        }
    }

    public void interact(ServerLevel level) {
        level.getBlockEntity(this.position, BlockEntityTypes.FURNACE).ifPresent(furnaceBe -> {
            SingleRecipeInput input = new SingleRecipeInput(furnaceBe.getItem(0));
            ((AbstractFurnaceBlockEntityAccessor) furnaceBe).cooking_frenzy$getQuickCheck().getRecipeFor(input, level).ifPresent(recipeHolder -> {
                ItemStack recipe = recipeHolder.value().assemble(input);
                if (!recipe.isEmpty()) {
                    Item item = furnaceBe.getItem(0).getItem();
                    boolean isOre = item.equals(Items.RAW_IRON) || item.equals(Items.RAW_GOLD);
                    this.timer = Math.round(((isOre) ? 15 * SharedConstants.TICKS_PER_SECOND : 30 * SharedConstants.TICKS_PER_SECOND) * this.timerMultiplier);
                    this.maxTimer = timer;
                    if (this.debugMode) {
                        System.out.println("Furnace Timer: " + timer);
                    }
                    this.isMaking = true;
                    level.setBlock(this.position, Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, orientation.front()).setValue(BlockStateProperties.LIT, true), 2);
                    level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, true).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
                }
            });
        });
    }
}
