package me.ellieis.cooking_frenzy.phases;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import me.ellieis.cooking_frenzy.CookingFrenzy;
import me.ellieis.cooking_frenzy.behaviours.*;
import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.map.Lobby;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.player.GamePlayerJoiner;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.registry.PlasmidRegistryKeys;
import xyz.nucleoid.plasmid.impl.game.manager.GameSpaceManagerImpl;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;

public class CookingFrenzyLobby extends CookingFrenzyPhase<Lobby> {
    Scheduler scheduler;
    CustomerBehaviour<Lobby> customerBehaviour;
    TemplateRegion customerButton;
    public void phaseRules(GameActivity activity) {
        activity.allow(GameRuleType.INTERACTION);
        activity.deny(GameRuleType.CRAFTING);
        activity.allow(GameRuleType.BLOCK_DROPS);
        activity.listen(EntityDamageEvent.EVENT, (_entity, _source, _amount) -> EventResult.DENY);
    }
    public CookingFrenzyLobby(CookingFrenzyConfig config, GameActivity activity, ServerLevel level, Lobby map) {
        super(config, activity, level, map, GameState.defaultGameState());
        spawnFloatingText("welcome", Component.translatable("cooking_frenzy.lobby.welcome"));
        spawnFloatingText("crafting_text", Component.translatable("cooking_frenzy.lobby.crafting"));
        spawnFloatingText("freezer_text", Component.translatable("cooking_frenzy.lobby.freezer"));
        spawnFloatingText("farming_text", Component.translatable("cooking_frenzy.lobby.farming"));
        spawnFloatingText("customer_text", Component.translatable("cooking_frenzy.lobby.customers"));
        this.customerButton = map.data.getFirstRegion("customer_button");
        this.scheduler = new Scheduler(0);
        for (RecipeMaker recipeMaker : map.getRecipeMakers(RecipeMaker.RecipeMakerType.CRAFTER)) {
            recipeMaker.unlock(level);
        }
        for (RecipeMaker recipeMaker : map.getRecipeMakers(RecipeMaker.RecipeMakerType.FURNACE)) {
            recipeMaker.unlock(level);
        }
        activity.listen(BlockBreakEvent.EVENT, (ServerPlayer _player, ServerLevel _level, BlockPos pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            if (FarmingBehaviour.isPlant(block) || block.equals(Blocks.MELON) || block.equals(Blocks.PUMPKIN)) {
                return EventResult.PASS;
            } else {
                return EventResult.DENY;
            }
        });
        new RecipeMakerBehaviour(activity.getGameSpace(), activity, level, map, false);
        new FreezerBehaviour(activity.getGameSpace(), activity, level, map, scheduler, new GameModifiers(), false);
        this.customerBehaviour = new CustomerBehaviour<>(activity.getGameSpace(), activity, this, false);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        GameWaitingLobby.addTo(activity, config.playerConfig());
        phaseRules(activity);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private @Nullable GameResult requestStart() {
        CookingFrenzyActive.Open(this.gameSpace, this.config, this.playerIntents);
        this.gameSpace.getLevels().remove(this.level);
        return GameResult.ok();
    }
    private void spawnFloatingText(String region, Component text) {
        Vec3 pos = map.data.getFirstRegion(region).getBounds().center();
        TextDisplayElement display = new TextDisplayElement(text);
        display.setBillboardMode(Display.BillboardConstraints.CENTER);
        display.setScale(new Vector3f(0.5f, 0.5f,0.5f));
        ElementHolder holder = new ElementHolder();
        holder.addElement(display);
        ChunkAttachment.of(holder, level, pos);
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LecternBlock) {
            if (map.getTutorialLectern().getBounds().contains(hitResult.getBlockPos())) {
                GameSpaceManagerImpl.get().open(level.registryAccess().lookupOrThrow(PlasmidRegistryKeys.GAME_CONFIG).get(CookingFrenzy.identifier("tutorial")).orElseThrow()).handleAsync((tutorial, throwable) -> {
                    tutorial.addPlayerFilter(playerRef -> playerRef.id().equals(player.getUUID()));
                    if (throwable == null) {
                        gameSpace.getPlayers().kick(player);
                        GamePlayerJoiner.tryJoin(player, tutorial, JoinIntent.PLAY);
                    } else {
                        player.sendSystemMessage(Component.translatable("cooking_frenzy.tutorial.open_failed"));
                    }
                    return null;
                });
            } else {
                ShopBehaviour.openStaticShop(player);
            }
            return InteractionResult.FAIL;
        }
        if (customerButton.getBounds().contains(hitResult.getBlockPos())) {
            customerBehaviour.spawnCustomer(false);
        }
        return InteractionResult.PASS;
    }

    public static GameOpenProcedure Open(GameOpenContext<CookingFrenzyConfig> context) {
        CookingFrenzyConfig config = context.config();
        MinecraftServer server = context.server();
        Lobby map = new Lobby(server, GameState.defaultGameState().currentModifiers(), config.debugMode());
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig().setGenerator(map.asChunkGenerator());
        return context.openWithLevel(levelConfig, (activity, level) -> new CookingFrenzyLobby(config, activity, level, map));
    }
}
