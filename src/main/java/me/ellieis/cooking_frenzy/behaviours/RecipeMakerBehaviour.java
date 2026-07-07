package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.map.MapWithRecipeMaker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.phys.BlockHitResult;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;

import java.util.ArrayList;

public class RecipeMakerBehaviour extends BaseBehaviour{
    MapWithRecipeMaker map;
    ServerLevel level;
    public RecipeMakerBehaviour(GameSpace gameSpace, GameActivity activity, ServerLevel level, MapWithRecipeMaker map, boolean debugMode) {
        super(gameSpace, activity, debugMode);
        this.map = map;
        this.level = level;
    }
    void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult blockHitResult) {
        Block block = this.level.getBlockState(blockHitResult.getBlockPos()).getBlock();
        if (block instanceof ButtonBlock) {
            ArrayList<RecipeMaker> crafters = map.getRecipeMakers(RecipeMaker.RecipeMakerType.CRAFTER);
            ArrayList<RecipeMaker> furnaces = map.getRecipeMakers(RecipeMaker.RecipeMakerType.FURNACE);
            for (RecipeMaker recipeMaker : crafters) {
                if (recipeMaker.isUnlocked() && !recipeMaker.isMaking() && recipeMaker.buttonPos().equals(blockHitResult.getBlockPos())) {
                    recipeMaker.interact(this.level);
                    return InteractionResult.FAIL;
                }
            }
            for (RecipeMaker recipeMaker : furnaces) {
                if (recipeMaker.isUnlocked() && !recipeMaker.isMaking() && recipeMaker.buttonPos().equals(blockHitResult.getBlockPos())) {
                    recipeMaker.interact(this.level);
                    return InteractionResult.FAIL;
                }
            }
        } else if (block instanceof CrafterBlock || block instanceof AbstractFurnaceBlock) {
            ArrayList<RecipeMaker> crafters = map.getRecipeMakers(RecipeMaker.RecipeMakerType.CRAFTER);
            ArrayList<RecipeMaker> furnaces = map.getRecipeMakers(RecipeMaker.RecipeMakerType.FURNACE);

            for (RecipeMaker recipeMaker : crafters) {
                if (recipeMaker.isMaking() && recipeMaker.position().equals(blockHitResult.getBlockPos())) {
                    return InteractionResult.FAIL;
                }
            }
            for (RecipeMaker recipeMaker : furnaces) {
                if (recipeMaker.isMaking() && recipeMaker.position().equals(blockHitResult.getBlockPos())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private void onTick() {
        ArrayList<RecipeMaker> crafters = this.map.getRecipeMakers(RecipeMaker.RecipeMakerType.CRAFTER);
        ArrayList<RecipeMaker> furnaces = this.map.getRecipeMakers(RecipeMaker.RecipeMakerType.FURNACE);
        for (RecipeMaker recipeMaker : crafters) {
            recipeMaker.tickTimer(this.level);
        }
        for (RecipeMaker recipeMaker : furnaces) {
            recipeMaker.tickTimer(this.level);
        }
    }
}
