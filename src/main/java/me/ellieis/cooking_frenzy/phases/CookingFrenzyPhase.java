package me.ellieis.cooking_frenzy.phases;

import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.map.Map;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockRandomTickEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;

import java.util.HashMap;

public abstract class CookingFrenzyPhase<T extends Map> {
    public final CookingFrenzyConfig config;
    public final GameSpace gameSpace;
    public final ServerLevel level;
    public final T map;
    public GameState gameState;
    public HashMap<ServerPlayer, JoinIntent> playerIntents = new HashMap<>();
    public boolean debugMode;
    public Scheduler scheduler;
    public static void baseRules(GameActivity activity) {
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.deny(GameRuleType.PVP);
        activity.deny(GameRuleType.FLUID_FLOW);
        activity.deny(GameRuleType.CORAL_DEATH);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.PORTALS);
        activity.listen(PlayerAttackEntityEvent.EVENT, (_plr, _hand, _entity, _result) -> EventResult.DENY);
        activity.listen(BlockRandomTickEvent.EVENT, (_block, _pos, _state) -> EventResult.DENY);
    }

    public abstract void phaseRules(GameActivity activity);

    public CookingFrenzyPhase(CookingFrenzyConfig config, GameActivity activity, ServerLevel level, T map, GameState state) {
        this.config = config;
        this.gameSpace = activity.getGameSpace();
        this.gameState = state;
        this.level = level;
        this.map = map;
        this.debugMode = config.debugMode();
        this.scheduler = new Scheduler(0);
        baseRules(activity);
        phaseRules(activity);
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            this.map.spawnPlayer(level, player);
        }
        for (ServerPlayer spectator : this.gameSpace.getPlayers().spectators()) {
            spectator.setGameMode(GameType.SPECTATOR);
        }
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor acceptor) {
        return acceptor.teleport(this.level, Vec3.ZERO).thenRunForEach((player, intent) -> {
            this.map.spawnPlayer(this.level, player);
            if (intent.canSpectate()) {
                player.setGameMode(GameType.SPECTATOR);
            }
            this.playerIntents.put(player, intent);
        });
    }
}
