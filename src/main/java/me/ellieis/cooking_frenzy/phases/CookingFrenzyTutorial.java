package me.ellieis.cooking_frenzy.phases;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.cooking_frenzy.behaviours.TutorialBehaviour;
import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.map.Lobby;
import me.ellieis.cooking_frenzy.map.Tutorial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;

import java.util.HashMap;

public class CookingFrenzyTutorial extends CookingFrenzyPhase<Tutorial> {
    public CookingFrenzyTutorial(CookingFrenzyConfig config, GameActivity activity, ServerLevel level, Tutorial map, GameState state) {
        super(config, activity, level, map, state);
        activity.listen(ItemThrowEvent.EVENT, (player, slot, item) -> EventResult.DENY);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(ItemUseEvent.EVENT, this::onItemUse);
        for (ServerPlayer player : gameSpace.getPlayers()) {
            player.getInventory().clearContent();
            ItemStack itemStack = new ItemStack(Items.BED.red());
            itemStack.set(DataComponents.CUSTOM_NAME, Component.translatable("text.plasmid.game.waiting_lobby.leave_game"));
            player.getInventory().add(8, itemStack);
        }
    }

    public CookingFrenzyTutorial(GameSpace gameSpace, CookingFrenzyConfig config, GameActivity activity, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        Tutorial map = new Tutorial(gameSpace.getServer());
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig().setGenerator(map.asChunkGenerator());
        ServerLevel level = gameSpace.getLevels().add(levelConfig);
        super(config, activity, level, map, state);
        this.playerIntents = playerIntents;
        activity.listen(ItemThrowEvent.EVENT, (player, slot, item) -> EventResult.DENY);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(ItemUseEvent.EVENT, this::onItemUse);
        for (ServerPlayer player : gameSpace.getPlayers()) {
            player.getInventory().clearContent();
            ItemStack itemStack = new ItemStack(Items.BED.red());
            itemStack.set(DataComponents.CUSTOM_NAME, Component.translatable("text.plasmid.game.waiting_lobby.leave_game"));
            player.getInventory().add(8, itemStack);
        }
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem().equals(Items.BED.red())) {
            gameSpace.getPlayers().kick(player);
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LecternBlock) {
            openGuiForPlayer(player);
        }
        return InteractionResult.PASS;
    }

    private void openGuiForPlayer(ServerPlayer player) {
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.translatable("cooking_frenzy.tutorial"));
        gui.setSlot(12, GuiElementBuilder.from(new ItemStack(Items.KNOWLEDGE_BOOK)).setName(Component.translatable("cooking_frenzy.tutorial.order")).setCallback(() -> {
            CookingFrenzyActive.OpenWithTutorial(gameSpace, config, playerIntents, TutorialBehaviour.TutorialType.ORDER);
            this.gameSpace.getLevels().remove(this.level);
        }).build());
        gui.setSlot(13, GuiElementBuilder.from(new ItemStack(Items.KNOWLEDGE_BOOK)).setName(Component.translatable("cooking_frenzy.tutorial.cooking")).setCallback(() -> {
            CookingFrenzyActive.OpenWithTutorial(gameSpace, config, playerIntents, TutorialBehaviour.TutorialType.COOKING);
            this.gameSpace.getLevels().remove(this.level);
        }).build());
        gui.setSlot(14, GuiElementBuilder.from(new ItemStack(Items.KNOWLEDGE_BOOK)).setName(Component.translatable("cooking_frenzy.tutorial.crafting")).setCallback(() -> {
            CookingFrenzyActive.OpenWithTutorial(gameSpace, config, playerIntents, TutorialBehaviour.TutorialType.CRAFTING);
            this.gameSpace.getLevels().remove(this.level);
        }).build());
        gui.open();
    }

    @Override
    public void phaseRules(GameActivity activity) {
        activity.allow(GameRuleType.INTERACTION);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.BLOCK_DROPS);
        activity.listen(EntityDamageEvent.EVENT, (entity, source, amount) -> EventResult.DENY);
    }

    public static void Open(GameSpace gameSpace, CookingFrenzyConfig config, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        gameSpace.setActivity(activity -> new CookingFrenzyTutorial(gameSpace, config, activity, state, playerIntents));
    }
    public static GameOpenProcedure Open(GameOpenContext<CookingFrenzyConfig> context) {
        CookingFrenzyConfig config = context.config();
        MinecraftServer server = context.server();
        Tutorial map = new Tutorial(server);
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig().setGenerator(map.asChunkGenerator());
        return context.openWithLevel(levelConfig, (activity, level) -> {
            new CookingFrenzyTutorial(config, activity, level, map, GameState.defaultGameState());
        });
    }
}
