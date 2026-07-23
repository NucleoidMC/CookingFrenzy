package me.ellieis.cooking_frenzy.gamestate;

import com.mojang.math.Transformation;
import me.ellieis.cooking_frenzy.ui.ProgressBarComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

public abstract class RecipeMaker {
    protected boolean isUnlocked;
    protected boolean isMain;
    protected boolean isMaking;
    protected BlockPos position;
    protected FrontAndTop orientation;
    // this is the block above the recipemaker that gives the status
    protected BlockPos indicatorPos;
    protected BlockPos buttonPos;
    protected BlockPos workingIndicatorPos;
    protected BlockState blockState;
    protected Display.TextDisplay timerDisplay;
    protected boolean isWorking = true;
    boolean blinking = false;
    int maxTimer;
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
        this.workingIndicatorPos = this.position.relative(this.orientation().front()).below();
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

    public void spawnTimer(ServerLevel level) {
        this.timerDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        timerDisplay.setBackgroundColor(0);
        Vec3 pos = Vec3.atCenterOf(this.position().relative(this.orientation.front())).relative(orientation.front(), -0.48);
        timerDisplay.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), this.orientation().front().toYRot(), 0, false);
        timerDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.4f, 0.4f, 0.1f), new Quaternionf()));
        level.addFreshEntity(timerDisplay);
    }

    public void updateTimer(ServerLevel level, boolean isWorking) {
        if (this.timerDisplay != null) {
            if (this.timer == 0) {
                timerDisplay.setText(Component.empty());
            } else {
                if (!isWorking) {
                    if (this.timer % 20 == 0) {
                        blinking = !blinking;
                        if (blinking) {
                            timerDisplay.setText(ProgressBarComponent.create(8, maxTimer - timer, 0, maxTimer, true, ChatFormatting.RED, ChatFormatting.WHITE));
                        } else {
                            timerDisplay.setText(Component.empty());
                        }
                    } else {
                        timerDisplay.setText(Component.empty());
                    }
                } else {
                    timerDisplay.setText(ProgressBarComponent.create(8, maxTimer - timer, 0, maxTimer, true, ChatFormatting.AQUA, ChatFormatting.WHITE));
                }

            }
        }
    }

    public void setIsWorking(ServerLevel level, boolean val) {
        this.isWorking = val;
        level.setBlock(this.workingIndicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState().setValue(CopperBulbBlock.LIT, this.isWorking), 2);
    }

    public void unlock(ServerLevel level) {
        this.isUnlocked = true;
        level.setBlock(this.position, this.blockState, 2);
        level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState(), 2);
        level.setBlock(this.buttonPos, Blocks.SPRUCE_BUTTON.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, this.orientation.front()), 2);
        level.setBlock(this.workingIndicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState().setValue(CopperBulbBlock.LIT, true), 2);
    }

    public void tickTimer(ServerLevel level) {
        if (this.timerDisplay == null) {
            this.spawnTimer(level);
        }
        if (this.isMaking) {
            if (this.isWorking) {
                if (this.timer > 0) {
                    this.timer--;
                    level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState().setValue(CopperBulbBlock.LIT, true), 2);
                    this.internalLoop(level);
                } else {
                    this.isMaking = false;
                    this.maxTimer = 0;
                    level.setBlock(this.indicatorPos, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState(), 2);
                    this.onMake(level);
                }
            }
            this.updateTimer(level, this.isWorking);
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