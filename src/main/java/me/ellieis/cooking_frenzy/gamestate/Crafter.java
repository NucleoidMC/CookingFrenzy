package me.ellieis.cooking_frenzy.gamestate;

import me.ellieis.cooking_frenzy.events.ItemCraftedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.stimuli.Stimuli;

public class Crafter extends RecipeMaker {
    public Crafter(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, FrontAndTop orientation, int timer, float timerMultiplier, boolean debugMode) {
        super(isUnlocked, isMain, isMaking, position, orientation, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, orientation), timer, timerMultiplier, debugMode);
    }

    protected void onMake(ServerLevel level) {
        level.getBlockEntity(this.position, BlockEntityTypes.CRAFTER).ifPresent(crafterBe -> {
            CraftingInput currentInv = crafterBe.asCraftInput();
            CrafterBlock.getPotentialResults(level, currentInv).ifPresent(recipeHolder -> {
                ItemStack recipe = recipeHolder.value().assemble(currentInv);
                if (!recipe.isEmpty()) {
                    Direction direction = this.orientation.front();
                    DefaultDispenseItemBehavior.spawnItem(level, recipe, 6, direction, Vec3.atCenterOf(this.position).relative(direction, 0.7));

                    for (ItemStack item : crafterBe.getItems()) {
                        item.shrink(1);
                    }
                    level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, false).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
                    level.playSound(null, this.position, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1, 1.8f);
                    Stimuli.select().at(level, this.position).get(ItemCraftedEvent.EVENT).onItemCrafted(recipe);
                }
            });
        });
    }

    public void internalLoop(ServerLevel level) {
        if (this.timer % 20 == 0) {
            level.playSound(null, this.position, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1, 0.5f);
        }
    }

    public void interact(ServerLevel level) {
        CrafterBlockEntity be = level.getBlockEntity(this.position, BlockEntityTypes.CRAFTER).get();
        if (CrafterBlock.getPotentialResults(level, be.asCraftInput()).isPresent()) {
            // 5 seconds per item
            this.timer = Math.round((be.asCraftInput().items().size() * 100) * this.timerMultiplier);
            this.maxTimer = timer;
            if (this.debugMode) {
                System.out.println("Crafter Timer: " + timer);
            }
            this.isMaking = true;
            level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, true).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
        }
    }
}
