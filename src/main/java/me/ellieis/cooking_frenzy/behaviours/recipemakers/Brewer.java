package me.ellieis.cooking_frenzy.behaviours.recipemakers;

import com.mojang.math.Transformation;
import me.ellieis.cooking_frenzy.behaviours.brewer_recipes.BrewerRecipes;
import me.ellieis.cooking_frenzy.behaviours.brewer_recipes.PotionRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.map_templates.TemplateRegion;

import java.util.List;

public class Brewer extends RecipeMaker {
    TemplateRegion potionSlot;
    TemplateRegion brewingSlot;
    BlockPos recipeListPos;
    BlockPos result;
    Result currentRecipe = null;
    public RecipeMakerType recipeMakerType = RecipeMakerType.BREWER;
    AnimationState animationState = AnimationState.NONE;
    Vec3 splashParticleOffset = new Vec3(0, 0, 0);
    Vec3 smokeParticleOffset = new Vec3(0, 0, 0);
    float smokeSinCount = 0;
    public Brewer(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, TemplateRegion potionSlot, TemplateRegion brewingSlot, BlockPos recipeListPos, BlockPos result, FrontAndTop orientation, int timer, float timerMultiplier, boolean debugMode) {
        super(isUnlocked, isMain, isMaking, position, orientation, null, position.relative(orientation.front()), position.relative(orientation.front()).below(), Blocks.POLISHED_TUFF.defaultBlockState(), timer, timerMultiplier, debugMode);
        this.potionSlot = potionSlot;
        this.brewingSlot = brewingSlot;
        this.recipeListPos = recipeListPos;
        this.result = result;
    }

    private void clearItems(ServerLevel level) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, potionSlot.getBounds().asBox())) {
            item.remove(Entity.RemovalReason.KILLED);
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, brewingSlot.getBounds().asBox())) {
            item.remove(Entity.RemovalReason.KILLED);
        }
    }

    private Result getSlotItems(ServerLevel level) {
        PotionContents potion = null;
        ItemStack recipe = null;
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, potionSlot.getBounds().asBox());
        if (!entities.isEmpty()) {
            for (ItemEntity entity : entities) {
                ItemStack item = entity.getItem();
                if (item.has(DataComponents.POTION_CONTENTS)) {
                    potion = item.get(DataComponents.POTION_CONTENTS);
                    break;
                }
            }
        }
        entities = level.getEntitiesOfClass(ItemEntity.class, brewingSlot.getBounds().asBox());
        if (!entities.isEmpty()) {
            for (ItemEntity entity : entities) {
                recipe = entity.getItem();
                break;
            }
        }
        return new Result(potion, recipe);
    }

    @Override
    public void unlock(ServerLevel level) {
        super.unlock(level);
        Vec3 basePos = Vec3.atCenterOf(recipeListPos).add(0, 0.5, 0);
        float yOffset = 0;
        Transformation scale = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.4f, 0.4f, 0.1f), new Quaternionf());
        for (PotionRecipe recipe : BrewerRecipes.recipes) {
            Vec3 baseLinePos = basePos.subtract(0.5, yOffset, 0.49);
            Display.ItemDisplay potion = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            potion.setItemStack(recipe.basePotionDisplay());
            potion.setPos(baseLinePos);
            potion.setTransformation(scale);
            Display.TextDisplay plusSymbol = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
            plusSymbol.setText(Component.literal("+"));
            plusSymbol.setPos(baseLinePos.add(0.35f, -0.1, 0));
            plusSymbol.setTransformation(scale);
            Display.ItemDisplay ingredient = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            ingredient.setItemStack(recipe.ingredientDisplay());
            ingredient.setPos(baseLinePos.add(0.7f, 0, 0));
            ingredient.setTransformation(scale);
            Display.TextDisplay equalsSymbol = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
            equalsSymbol.setText(Component.literal("="));
            equalsSymbol.setPos(baseLinePos.add(1f, -0.1, 0));
            equalsSymbol.setTransformation(scale);
            Display.ItemDisplay result = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            result.setItemStack(recipe.resultDisplay());
            result.setPos(baseLinePos.add(1.3f, 0, 0));
            result.setTransformation(scale);
            level.addFreshEntity(potion);
            level.addFreshEntity(plusSymbol);
            level.addFreshEntity(ingredient);
            level.addFreshEntity(equalsSymbol);
            level.addFreshEntity(result);
            yOffset += 0.5f;
        }
    }

    protected void onMake(ServerLevel level) {
        if (currentRecipe.isValid()) {
            ItemStack item = BrewerRecipes.getResult(currentRecipe.potion(), currentRecipe.recipe());
            Vec3 pos = Vec3.atCenterOf(result);
            level.addFreshEntity(new ItemEntity(level, pos.x(), pos.y(), pos.z(), item));
            level.playSound(null, this.position, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.BLOCKS, 1, 1);
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x(), pos.y(), pos.z(), 10, 0.1, 0.1 ,0.1, 0.01);
            level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, false).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
            animationState = AnimationState.NONE;
            smokeParticleOffset = Vec3.ZERO;
            splashParticleOffset = Vec3.ZERO;
            smokeSinCount = 0;
        }
    }

    public void internalLoop(ServerLevel level) {
        if (this.timer % 2 == 0) {
            if (this.timer % 10 == 0) {
                level.playSound(null, result, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 1, 1);
            }
            if (animationState == AnimationState.RISING) {
                splashParticleOffset = splashParticleOffset.add(0, 0.1, 0);
                if (splashParticleOffset.y() > 3.25) {
                    animationState = AnimationState.CONVERGING;
                }
            } else if (animationState == AnimationState.CONVERGING) {
                splashParticleOffset = splashParticleOffset.add(0.1, 0, 0);
                if (splashParticleOffset.x() > 2.3) {
                    animationState = AnimationState.FALLING;
                }
            } else if (animationState == AnimationState.FALLING) {
                if (splashParticleOffset.y() > 0) {
                    splashParticleOffset = splashParticleOffset.subtract(0, 0.1, 0);
                } else {
                    if (this.timer < 20) {
                        splashParticleOffset = splashParticleOffset.subtract(0, 0.2, 0);
                    } else {
                        smokeSinCount += 0.4f;
                        smokeParticleOffset = new Vec3(Math.sin(smokeSinCount), 3, Math.cos(smokeSinCount));
                        Vec3 pos = Vec3.atCenterOf(result).add(smokeParticleOffset);
                        level.sendParticles(ParticleTypes.FLAME, pos.x(), pos.y(), pos.z(), 10, 0.1, 0.1, 0.1, 0);
                        if (this.timer % 10 == 0) {
                            level.playSound(null, result, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1, 1);
                        }
                    }
                }
            }

            Vec3 potionPos = potionSlot.getBounds().center().add(-splashParticleOffset.x(), Math.min(3, splashParticleOffset.y()), splashParticleOffset.z());
            Vec3 brewingPos = brewingSlot.getBounds().center().add(splashParticleOffset);
            level.sendParticles(ParticleTypes.SPLASH, potionPos.x(), potionPos.y(), potionPos.z(), 10, 0.1, 0.1, 0.1, 0);
            level.sendParticles(ParticleTypes.SPLASH, brewingPos.x(), brewingPos.y(), brewingPos.z(), 10, 0.1, 0.1, 0.1, 0);
        }
    }

    public void interact(ServerLevel level) {
        Result result = getSlotItems(level);
        if (result.isValid()) {
            if (BrewerRecipes.isValidRecipe(result.potion(), result.recipe())) {
                clearItems(level);
                currentRecipe = result;
                this.timer = 15 * SharedConstants.TICKS_PER_SECOND;
                this.maxTimer = timer;
                this.isMaking = true;
                this.animationState = AnimationState.RISING;
                level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(BlockStateProperties.POWERED, true).setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
            }
        }
    }

    record Result(PotionContents potion, ItemStack recipe) {
        boolean isValid() {
            return potion != null && recipe != null;
        }
    }

    enum AnimationState {
        NONE,
        RISING,
        CONVERGING,
        FALLING

    }
}
