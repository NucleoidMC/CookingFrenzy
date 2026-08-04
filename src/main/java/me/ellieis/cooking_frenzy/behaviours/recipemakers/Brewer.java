package me.ellieis.cooking_frenzy.behaviours.recipemakers;

import me.ellieis.cooking_frenzy.behaviours.brewer_recipes.BrewerRecipes;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Brewer extends RecipeMaker {
    BlockPos potionSlot;
    BlockPos brewingSlot;
    BlockPos result;
    Result currentRecipe = null;
    public Brewer(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, BlockPos potionSlot, BlockPos brewingSlot, BlockPos result, FrontAndTop orientation, int timer, float timerMultiplier, boolean debugMode) {
        super(isUnlocked, isMain, isMaking, position, orientation, null, position.relative(orientation.front()), position.relative(orientation.front()).below(), Blocks.POLISHED_TUFF.defaultBlockState(), timer, timerMultiplier, debugMode);
        this.potionSlot = potionSlot;
        this.brewingSlot = brewingSlot;
        this.result = result;
    }

    private void clearItems(ServerLevel level) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(potionSlot))) {
                item.remove(Entity.RemovalReason.KILLED);
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(brewingSlot))) {
            item.remove(Entity.RemovalReason.KILLED);
        }
    }

    private Result getSlotItems(ServerLevel level) {
        PotionContents potion = null;
        ItemStack recipe = null;
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(potionSlot));
        if (!entities.isEmpty()) {
            for (ItemEntity entity : entities) {
                ItemStack item = entity.getItem();
                if (item.has(DataComponents.POTION_CONTENTS)) {
                    potion = item.get(DataComponents.POTION_CONTENTS);
                    break;
                }
            }
        }
        entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(brewingSlot));
        if (!entities.isEmpty()) {
            for (ItemEntity entity : entities) {
                recipe = entity.getItem();
                break;
            }
        }
        return new Result(potion, recipe);
    }
    protected void onMake(ServerLevel level) {
        if (currentRecipe.isValid()) {
            ItemStack item = BrewerRecipes.getResult(currentRecipe.potion(), currentRecipe.recipe());
            Vec3 pos = Vec3.atCenterOf(result);
            level.addFreshEntity(new ItemEntity(level, pos.x(), pos.y(), pos.z(), item));
            level.playSound(null, this.position, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1, 1.8f);
            level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, false).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
        }
    }

    public void internalLoop(ServerLevel level) {
        if (this.timer % 20 == 0) {
            level.playSound(null, this.position, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1, 0.5f);
        }
    }

    public void interact(ServerLevel level) {
        Result result = getSlotItems(level);
        if (result.isValid()) {
            if (BrewerRecipes.isValidRecipe(result.potion(), result.recipe())) {
                clearItems(level);
                currentRecipe = result;
                this.timer = 30 * SharedConstants.TICKS_PER_SECOND;
                this.maxTimer = timer;
                this.isMaking = true;
                level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, true).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
            }
        }
    }

    record Result(PotionContents potion, ItemStack recipe) {
        boolean isValid() {
            return potion != null && recipe != null;
        }
    }
}
