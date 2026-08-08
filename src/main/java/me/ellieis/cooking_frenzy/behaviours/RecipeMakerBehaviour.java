package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.behaviours.malfunctions.MalfunctionType;
import me.ellieis.cooking_frenzy.behaviours.recipemakers.RecipeMaker;
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
import java.util.List;

public class RecipeMakerBehaviour extends DisableableBehaviour {
    MapWithRecipeMaker map;
    ServerLevel level;
    boolean isFurnaceDisabled = false;
    boolean isCrafterDisabled = false;
    public RecipeMakerBehaviour(GameSpace gameSpace, GameActivity activity, ServerLevel level, MapWithRecipeMaker map, boolean debugMode) {
        super(gameSpace, activity, debugMode, List.of(MalfunctionType.LIGHTS, MalfunctionType.KITCHEN_FIRE), true);
        this.map = map;
        this.level = level;
    }
    protected void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult blockHitResult) {
        Block block = this.level.getBlockState(blockHitResult.getBlockPos()).getBlock();
        if (block instanceof ButtonBlock) {
            ArrayList<RecipeMaker> recipeMakers = map.getAllRecipeMakers();
            for (RecipeMaker recipeMaker : recipeMakers) {
                if (recipeMaker.isUnlocked() && !recipeMaker.isMaking() && recipeMaker.buttonPos().equals(blockHitResult.getBlockPos())) {
                    if (!recipeMaker.isWorking) return InteractionResult.FAIL;
                    recipeMaker.interact(this.level);
                    return InteractionResult.FAIL;
                }
            }
        } else if (block instanceof CrafterBlock || block instanceof AbstractFurnaceBlock) {
            ArrayList<RecipeMaker> recipeMakers = map.getAllRecipeMakers();
            for (RecipeMaker recipeMaker : recipeMakers) {
                if ((recipeMaker.isMaking() || !recipeMaker.isWorking) && recipeMaker.position().equals(blockHitResult.getBlockPos())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private void onTick() {
        ArrayList<RecipeMaker> recipeMakers = map.getAllRecipeMakers();
        for (RecipeMaker recipeMaker : recipeMakers) {
            recipeMaker.tickTimer(this.level);
        }
    }

    void setEnabled(RecipeMaker.RecipeMakerType type, boolean val) {
        ArrayList<RecipeMaker> recipeMakers = map.getRecipeMakers(type);
        for (RecipeMaker recipeMaker : recipeMakers) {
            if (recipeMaker.isUnlocked()) {
                recipeMaker.setIsWorking(level, val);
            }
        }

        if (type == RecipeMaker.RecipeMakerType.CRAFTER) {
            isCrafterDisabled = !val;
        } else if (type == RecipeMaker.RecipeMakerType.FURNACE) {
            isFurnaceDisabled = !val;
        }
    }

    boolean shouldDisableCrafter() {
        return this.malfunctionsAffectingBehaviour.contains(MalfunctionType.LIGHTS);
    }

    boolean shouldDisableFurnace() {
        return this.malfunctionsAffectingBehaviour.contains(MalfunctionType.KITCHEN_FIRE) || this.malfunctionsAffectingBehaviour.contains(MalfunctionType.LIGHTS);
    }

    boolean shouldDisableBrewer(){
        return this.malfunctionsAffectingBehaviour.contains(MalfunctionType.LIGHTS);
    }

    @Override
    void onDisable(MalfunctionType reason) {
        // need to do this so the disable functions can know the reason
        this.malfunctionsAffectingBehaviour.add(reason);
        setEnabled(RecipeMaker.RecipeMakerType.FURNACE, !shouldDisableFurnace());
        setEnabled(RecipeMaker.RecipeMakerType.CRAFTER, !shouldDisableCrafter());
        setEnabled(RecipeMaker.RecipeMakerType.BREWER, !shouldDisableBrewer());
        // make sure to remove because it gets added by DisableableBehaviour automatically
        this.malfunctionsAffectingBehaviour.remove(reason);
    }

    @Override
    void onEnable(MalfunctionType reason) {
        setEnabled(RecipeMaker.RecipeMakerType.FURNACE, !shouldDisableFurnace());
        setEnabled(RecipeMaker.RecipeMakerType.CRAFTER, !shouldDisableCrafter());
        setEnabled(RecipeMaker.RecipeMakerType.BREWER, !shouldDisableBrewer());
    }
}
