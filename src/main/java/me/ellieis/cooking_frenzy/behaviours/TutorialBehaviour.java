package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.behaviours.extra.CookingTutorial;
import me.ellieis.cooking_frenzy.behaviours.extra.CraftingTutorial;
import me.ellieis.cooking_frenzy.behaviours.extra.OrderTutorial;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyTutorial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;

import java.awt.*;
import java.util.Set;

public class TutorialBehaviour extends BaseBehaviour {
    CookingFrenzyActive game;
    Active map;
    public ServerPlayer player;
    Vec3 oldPlayerPos;
    TutorialType currentTutorial;
    CustomerBehaviour<Active> customerBehaviour;
    FarmingBehaviour farmingBehaviour;
    boolean isSpectating = false;
    public Active.CamPos currentCamera;
    public long timeUntilAngleEnds;
    public TutorialBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game, CustomerBehaviour<Active> customerBehaviour, FarmingBehaviour farmingBehaviour, TutorialType currentTutorial, boolean debugMode) {
        super(gameSpace, activity, debugMode);
        this.game = game;
        this.map = game.map;
        this.customerBehaviour = customerBehaviour;
        this.farmingBehaviour = farmingBehaviour;
        this.currentTutorial = currentTutorial;
        gameSpace.getPlayers().forEach(player -> {
            ItemStack item = new ItemStack(Items.WOOL.green());
            item.set(DataComponents.CUSTOM_NAME, Component.translatable("cooking_frenzy.tutorial.start"));
            player.getInventory().add(item);
        });
    }

    @Override
    void setupEvents() {
        activity.listen(ItemUseEvent.EVENT, this::onItemUse);
        activity.listen(BlockPlaceEvent.BEFORE, this::onBlockPlace);
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    public void setCameraAngle(Active.CamPos camera, long time) {
        setCameraAngle(camera, time, player.position());
    }

    public void setCameraAngle(Active.CamPos camera, long time, Vec3 oldPlayerPos) {
        currentCamera = camera;
        timeUntilAngleEnds = time;
        this.oldPlayerPos = oldPlayerPos;
    }

    public void sendTutorialMessage(Component text) {
        player.sendSystemMessage(text.copy().withStyle(ChatFormatting.GREEN));
        PlayerUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1, 1);
    }

    private void startTutorial(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
        player.getInventory().clearContent();
        map.spawnPlayer(player.level(), player);
        this.player = player;
        switch (this.currentTutorial) {
            case ORDER -> {
                new OrderTutorial(this, activity, gameSpace, map, game.scheduler, game, customerBehaviour).startTutorial();
            }
            case CRAFTING -> {
                new CraftingTutorial(this, activity, gameSpace, map, game.scheduler, game, customerBehaviour, farmingBehaviour).startTutorial();
            }
            case COOKING -> {
                new CookingTutorial(this, activity, gameSpace, map, game.scheduler, game, customerBehaviour).startTutorial();
            }
            case SHOP -> {

            }
        }
    }

    public void endCurrentTutorial() {
        game.endGame(false);
    }

    private void onTick() {
        if (this.currentCamera != null) {
            if (this.timeUntilAngleEnds <= 0) {
                if (this.isSpectating) {
                    this.isSpectating = false;
                    player.setGameMode(GameType.SURVIVAL);
                    player.teleportTo(game.level, oldPlayerPos.x(), oldPlayerPos.y(), oldPlayerPos.z(), Set.of(), 0, 0, true);
                }
            } else {
                this.isSpectating = true;
                player.setGameMode(GameType.SPECTATOR);
                Vec3 pos = currentCamera.pos();
                player.teleportTo(game.level, pos.x(), pos.y() - 1.5, pos.z(), Set.of(), currentCamera.yaw(), currentCamera.pitch(), true);
                this.timeUntilAngleEnds--;
            }
        }
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem().equals(Items.WOOL.green())) {
            startTutorial(player);
        }
        return InteractionResult.PASS;
    }

    private EventResult onBlockPlace(ServerPlayer player, ServerLevel level, BlockPos blockPos, BlockState blockState, UseOnContext useOnContext) {
        if (blockState.getBlock().equals(Blocks.WOOL.green())) {
            startTutorial(player);
            return EventResult.DENY;
        }
        return EventResult.PASS;
    }
    public enum TutorialType {
        ORDER,
        COOKING,
        CRAFTING,
        SHOP
    }
}
