package me.ellieis.cooking_frenzy.behaviours.recipemakers;

import com.mojang.math.Transformation;
import me.ellieis.cooking_frenzy.behaviours.brewer_recipes.BrewerRecipes;
import me.ellieis.cooking_frenzy.behaviours.brewer_recipes.PotionRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.component.DataComponents;
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
    BlockPos brewerRecipePos;
    BlockPos result;
    Result currentRecipe = null;
    public RecipeMakerType recipeMakerType = RecipeMakerType.BREWER;
    public Brewer(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, TemplateRegion potionSlot, TemplateRegion brewingSlot, BlockPos brewerRecipePos, BlockPos result, FrontAndTop orientation, int timer, float timerMultiplier, boolean debugMode) {
        super(isUnlocked, isMain, isMaking, position, orientation, null, position.relative(orientation.front()), position.relative(orientation.front()).below(), Blocks.POLISHED_TUFF.defaultBlockState(), timer, timerMultiplier, debugMode);
        this.potionSlot = potionSlot;
        this.brewingSlot = brewingSlot;
        this.brewerRecipePos = brewerRecipePos;
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
        Vec3 basePos = Vec3.atCenterOf(brewerRecipePos);
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
                this.timer = 15 * SharedConstants.TICKS_PER_SECOND;
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
