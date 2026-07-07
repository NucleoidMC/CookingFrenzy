package me.ellieis.cooking_frenzy.gamestate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RecipeMaker {
    protected boolean isUnlocked;
    protected boolean isMain;
    protected boolean isMaking;
    protected BlockPos position;
    protected FrontAndTop orientation;
    // this is the block above the recipemaker that gives the status
    protected BlockPos indicatorPos;
    protected BlockPos buttonPos;
    protected BlockState blockState;
    int timer;
    float timerMultiplier;
    boolean debugMode;
    public RecipeMaker(boolean isUnlocked, boolean isMain, boolean isMaking, BlockPos position, FrontAndTop orientation, BlockState blockState, int timer, float timerMultiplier, boolean debugMode) {
        this.isUnlocked = isUnlocked;
        this.isMain = isMain;
        this.isMaking = isMaking;
        this.position = position;
        this.orientation = orientation;
        this.indicatorPos = this.position.above();
        this.buttonPos = this.indicatorPos.relative(this.orientation.front(), 1);
        this.blockState = blockState;
        this.timer = timer;
        this.timerMultiplier = timerMultiplier;
        this.debugMode = debugMode;
    }
    public boolean isUnlocked() {
        return this.isUnlocked;
    };
    public boolean isMain() {
        return this.isMain;
    };
    public boolean isMaking() {
        return this.isMaking;
    };
    public BlockPos position() {
        return this.position;
    };
    public BlockPos indicatorPos() { return this.indicatorPos; }
    public BlockPos buttonPos() { return this.buttonPos; }
    public BlockState blockState() { return this.blockState; }
    public FrontAndTop orientation() {
        return this.orientation;
    };
    public int timer() {
        return this.timer;
    };
    public float timerMultiplier() { return this.timerMultiplier; }

    public void unlock(ServerLevel level) {
        this.isUnlocked = true;
        level.setBlock(this.position, this.blockState, 2);
        level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState(), 2);
        level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
    }

    public void tickTimer(ServerLevel level) {
        if (this.isMaking) {
            if (this.timer > 0) {
                this.timer--;
                level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState().setValue(CopperBulbBlock.LIT, true), 2);
                this.internalLoop(level);
            } else {
                this.isMaking = false;
                level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState(), 2);
                this.onMake(level);
            }
        }
    }

    protected abstract void internalLoop(ServerLevel level);
    protected abstract void onMake(ServerLevel level);
    public abstract void interact(ServerLevel level);
    public enum RecipeMakerType {
        CRAFTER,
        FURNACE
    }
}