package me.ellieis.cooking_frenzy.behaviours.malfunctions;

import me.ellieis.cooking_frenzy.behaviours.BaseBehaviour;
import me.ellieis.cooking_frenzy.behaviours.DisableableBehaviour;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class KitchenFireBehaviour extends BaseBehaviour implements MalfunctionBehaviour {
    boolean isEnabled = true;
    CookingFrenzyActive game;
    ArrayList<BlockPos> fireSpots = new ArrayList<>();
    public KitchenFireBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game) {
        super(gameSpace, activity, game.debugMode);
        this.game = game;
    }
    void spawnFire() {
        fireSpots.clear();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (BlockPos bound : game.map.getKitchenFireArea().getBounds()) {
            if (game.level.getBlockState(bound) == Blocks.AIR.defaultBlockState()) {
                if (random.nextFloat() >= 0.7) {
                    fireSpots.add(bound.immutable());
                    game.level.setBlock(bound, Blocks.FIRE.defaultBlockState(), 2);
                }
            }
        }
        if (fireSpots.isEmpty()) {
            spawnFire();
        }
    }
    @Override
    protected void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        if (this.debugMode) {
            activity.listen(PlayerChatEvent.EVENT, this::onChat);
        }
    }

    private void onTick() {
        if (!this.isEnabled) {
            boolean isThereFire = false;
            for (BlockPos fireSpot : fireSpots) {
                if (game.level.getBlockState(fireSpot) == Blocks.FIRE.defaultBlockState()) {
                    isThereFire = true;
                    break;
                }
            }
            if (!isThereFire) {
                toggleMalfunction(true);
            }
        }
    }

    private EventResult onChat(ServerPlayer player, PlayerChatMessage playerChatMessage, ChatType.Bound bound) {
        if (playerChatMessage.decoratedContent().getString().equals("fire")) {
            toggleMalfunction(!isEnabled);
        }
        return EventResult.PASS;
    }

    @Override
    public void toggleMalfunction(boolean val) {
        this.isEnabled = val;
        for (DisableableBehaviour disableableBehaviour : game.getDisableableBehaviours(MalfunctionType.KITCHEN_FIRE)) {
            if (this.isEnabled) {
                disableableBehaviour.enableBehaviour(MalfunctionType.KITCHEN_FIRE);
            } else {
                disableableBehaviour.disableBehaviour(MalfunctionType.KITCHEN_FIRE);
                spawnFire();
            }
        }
    }

    @Override
    public Component getTitle() {
        return Component.translatable("cooking_frenzy.malfunctions.kitchen_fire");
    }

    @Override
    public Component getDesc() {
        return Component.translatable("cooking_frenzy.malfunctions.kitchen_fire.desc");
    }
}
