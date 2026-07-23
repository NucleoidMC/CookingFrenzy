package me.ellieis.cooking_frenzy.behaviours.malfunctions;

import me.ellieis.cooking_frenzy.behaviours.BaseBehaviour;
import me.ellieis.cooking_frenzy.behaviours.DisableableBehaviour;
import me.ellieis.cooking_frenzy.behaviours.extra.FreezerMaintenanceNPC;
import me.ellieis.cooking_frenzy.behaviours.extra.Node;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;

import java.util.ArrayList;

public class FreezerMaintenanceBehaviour extends BaseBehaviour implements MalfunctionBehaviour {
    CookingFrenzyActive game;
    ServerLevel level;
    FreezerMaintenanceNPC maintenanceGuy = null;
    boolean isEnabled = true;
    Vec3 spawn;
    ArrayList<Node> nodes = new ArrayList<>();
    public FreezerMaintenanceBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game) {
        super(gameSpace, activity, game.debugMode);
        this.game = game;
        this.level = game.level;
        this.spawn = game.map.getMaintenanceGuySpawn().getBounds().centerBottom();
        for (TemplateRegion maintenanceNode : game.map.getMaintenanceNodes()) {
            nodes.add(Node.fromRegion(maintenanceNode));
        }
    }
    @Override
    protected void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(EntityUseEvent.EVENT, this::onEntityUse);
        if (this.debugMode) {
            activity.listen(PlayerChatEvent.EVENT, this::onChat);
        }
    }

    private EventResult onChat(ServerPlayer player, PlayerChatMessage playerChatMessage, ChatType.Bound bound) {
        if (playerChatMessage.decoratedContent().getString().equals("maintenance")) {
            toggleMalfunction(!isEnabled);
        }
        return EventResult.PASS;
    }

    private EventResult onEntityUse(ServerPlayer player, Entity entity, InteractionHand hand, EntityHitResult entityHitResult) {
        if (maintenanceGuy != null) {
            if (entity.equals(maintenanceGuy.entity)) {
                maintenanceGuy.onInteract(player.getItemInHand(hand));
            }
        }
        return EventResult.PASS;
    }

    private void onTick() {
        if (maintenanceGuy != null) {
            if (maintenanceGuy.despawnFlag) {
                maintenanceGuy.entity.remove(Entity.RemovalReason.KILLED);
            } else {
                maintenanceGuy.tick();
                if (maintenanceGuy.isFixed && !this.isEnabled) {
                    toggleMalfunction(true);
                }
            }
        }
    }

    @Override
    public void toggleMalfunction(boolean val) {
        this.isEnabled = val;
        for (DisableableBehaviour disableableBehaviour : game.getDisableableBehaviours(MalfunctionType.FREEZER_MAINTENANCE)) {
            if (this.isEnabled) {
                disableableBehaviour.enableBehaviour(MalfunctionType.FREEZER_MAINTENANCE);
            } else {
                disableableBehaviour.disableBehaviour(MalfunctionType.FREEZER_MAINTENANCE);
                maintenanceGuy = new FreezerMaintenanceNPC(nodes, level, spawn, game);
            }
        }
    }

    @Override
    public Component getTitle() {
        return Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance");
    }

    @Override
    public Component getDesc() {
        return Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance.desc");
    }
}
