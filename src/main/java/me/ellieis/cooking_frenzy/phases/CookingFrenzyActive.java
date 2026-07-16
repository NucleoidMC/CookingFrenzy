package me.ellieis.cooking_frenzy.phases;

import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.*;
import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.gamestate.upgrades.BaseUpgrade;
import me.ellieis.cooking_frenzy.gamestate.upgrades.DebtUpgrade;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.scheduler.CountdownTask;
import me.ellieis.cooking_frenzy.scheduler.Task;
import me.ellieis.cooking_frenzy.ui.Common;
import me.ellieis.cooking_frenzy.ui.ProgressBarComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerInventoryActionEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class CookingFrenzyActive extends CookingFrenzyPhase<Active> implements PhaseWithState {
    public GlobalWidgets widgets;
    public SidebarWidget sidebar;
    public BossBarWidget timeBar;
    public BossBarWidget shopBar;
    CustomerBehaviour<Active> customerBehaviour;
    FarmingBehaviour farmingBehaviour;
    ShopBehaviour shopBehaviour;
    SongBehaviour songBehaviour;
    public HashMap<ServerPlayer, JoinIntent> playerIntents;
    public HashMap<ItemEntity, Long> itemThrownTime = new HashMap<>();
    ArrayList<BaseBehaviour> behaviours = new ArrayList<>();
    public long finishTime;
    public long time = 0;
    public int minMoney;
    public boolean gameOver = false;
    public boolean isTutorial;
    HashMap<BlockPos, BlockState> singlePlayerStates = new HashMap<>();
    HashMap<BlockPos, BlockState> multiPlayerStates = new HashMap<>();
    private CookingFrenzyActive(GameSpace gameSpace, CookingFrenzyConfig config, GameActivity activity, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents, boolean isTutorial, TutorialBehaviour.TutorialType tutorialType) {
        Active map = new Active(gameSpace.getServer(), state.currentModifiers(), playerIntents, config.debugMode());
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig().setGenerator(map.asChunkGenerator());
        ServerLevel level = gameSpace.getLevels().add(levelConfig);
        this.playerIntents = playerIntents;
        super(config, activity, level, map, state);
        map.setGame(this);
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(PlayerInventoryActionEvent.EVENT, this::inventoryChange);
        activity.listen(EntitySpawnEvent.EVENT, this::onEntitySpawn);
        activity.listen(EntityUseEvent.EVENT, this::onEntityUse);
        activity.listen(ItemPickupEvent.EVENT, this::onItemPickup);
        activity.listen(ItemThrowEvent.EVENT, this::onItemThrow);
        activity.listen(PlayerDeathEvent.EVENT, this::onDeath);
        activity.listen(BlockPlaceEvent.BEFORE, this::onBlockPlace);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(GamePlayerEvents.JOIN, this::onJoin);
        activity.listen(GamePlayerEvents.LEAVE, this::onLeave);
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
        // game state setup
        this.gameState = state;
        this.behaviours.add(new FreezerBehaviour(gameSpace, activity, level, map, scheduler, this.gameState.currentModifiers(), debugMode));
        this.customerBehaviour = new CustomerBehaviour<>(gameSpace, activity, this, !isTutorial);
        this.behaviours.add(this.customerBehaviour);
        this.farmingBehaviour = new FarmingBehaviour(gameSpace, activity, this);
        this.behaviours.add(this.farmingBehaviour);
        this.shopBehaviour = new ShopBehaviour(gameSpace, activity, this);
        this.behaviours.add(shopBehaviour);
        this.songBehaviour = new SongBehaviour(gameSpace, activity, this.debugMode, false);
        this.behaviours.add(songBehaviour);
        this.behaviours.add(new RecipeMakerBehaviour(gameSpace, activity, level, map, debugMode));
        this.widgets = GlobalWidgets.addTo(activity);
        this.sidebar = this.widgets.addSidebar(Component.translatable("gameType.cooking_frenzy.cooking_frenzy"));
        this.timeBar = this.widgets.addBossBar(Component.translatable("cooking_frenzy.time_left"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        this.shopBar = this.widgets.addBossBar(Component.translatable("cooking_frenzy.shop_delivery"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        this.shopBar.setVisible(false);
        this.isTutorial = isTutorial;
        if (isTutorial) {
            this.finishTime = SharedConstants.TICKS_PER_MINUTE * 999;
            behaviours.add(new TutorialBehaviour(gameSpace, activity, this, customerBehaviour, farmingBehaviour, tutorialType, false));
        } else {
            this.finishTime = SharedConstants.TICKS_PER_MINUTE * 8;
        }
        for (BaseUpgrade upgrade : this.gameState.upgrades()) {
            if (upgrade instanceof DebtUpgrade) {
                this.minMoney = -50;
                break;
            } else {
                this.minMoney = 0;
            }
        }
        this.scheduler.addTask(new Task(this.finishTime - 5 * SharedConstants.TICKS_PER_SECOND, () -> {
            songBehaviour.stopSongs();
            this.gameSpace.getPlayers().playSound(SoundEvent.createFixedRangeEvent(CustomSounds.DRUMROLL, 1000), SoundSource.UI, 1, 1);
        }));
        this.scheduler.addTask(new Task(this.finishTime, () -> endGame(false)));
        map.unlockMainRecipeMakers(level);
        for (int i = 0; i < this.gameState.crafterCount() - 1; i++) {
            map.unlockRecipeMaker(level, RecipeMaker.RecipeMakerType.CRAFTER);
        }
        for (int i = 0; i < this.gameState.furnaceCount() - 1; i++) {
            map.unlockRecipeMaker(level, RecipeMaker.RecipeMakerType.FURNACE);
        }
        checkSingleplayerStatus(false);
    }

    void updateWidgets() {
        this.timeBar.setProgress(1 - ((float) time) / this.finishTime);
        ArrayList<ShopBehaviour.QueuedItem> shopQueue = this.shopBehaviour.getShopQueue();
        if (!shopQueue.isEmpty()) {
            this.shopBar.setVisible(true);
            ShopBehaviour.QueuedItem mostRecentItem = null;
            for (ShopBehaviour.QueuedItem queuedItem : shopQueue) {
                if (mostRecentItem == null) {
                    mostRecentItem = queuedItem;
                } else if (queuedItem.time() <= mostRecentItem.time()) {
                    mostRecentItem = queuedItem;
                }
            }
            this.shopBar.setProgress(Common.mapRange(time, mostRecentItem.startedTime(), mostRecentItem.time(), 0, 1));
        } else {
            this.shopBar.setVisible(false);
        }
        this.sidebar.set(consumer -> {
            consumer.add(Component.empty());
            ArrayList<CustomerBehaviour.AvailableOrder> orders = this.customerBehaviour.getAvailableOrders();
            if (orders.isEmpty()) {
                consumer.add(Component.translatable("cooking_frenzy.sidebar.no_orders").withStyle(ChatFormatting.YELLOW));
                consumer.add(Component.empty());
            } else {
                consumer.add(Component.translatable("cooking_frenzy.sidebar.current_orders").withStyle(ChatFormatting.YELLOW));
                consumer.add(Component.literal(new StringBuilder().repeat("-", 21).toString()).withStyle(ChatFormatting.BLUE));
                for (CustomerBehaviour.AvailableOrder availableOrder : this.customerBehaviour.getAvailableOrders()) {
                    consumer.add(availableOrder.itemName());
                    consumer.add(ProgressBarComponent.create(14, availableOrder.timeLeft(), 0, availableOrder.timeLimit(), true));
                }
                consumer.add(Component.literal(new StringBuilder().repeat("-", 21).toString()).withStyle(ChatFormatting.BLUE));

            }
            consumer.add(Component.translatable("cooking_frenzy.sidebar.score", Component.literal("$" + this.gameState.money()).withStyle(this.gameState.money() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED)).withStyle(ChatFormatting.YELLOW));
            consumer.add(Component.translatable("cooking_frenzy.sidebar.time_until_customer").withStyle(ChatFormatting.YELLOW));
            consumer.add(ProgressBarComponent.create(14, customerBehaviour.timeSinceLastCustomer, 0, customerBehaviour.timeForCustomerSpawn, true));
            consumer.add(Component.translatable("cooking_frenzy.sidebar.day_counter", this.gameState.dayCount()).withStyle(ChatFormatting.YELLOW));
        });
    }
    public GameState getState() {
        return this.gameState;
    }
    public void setState(GameState state) {
        this.gameState = state;
    }

    private void checkSingleplayerStatus(boolean playerLeaving) {
        if (this.singlePlayerStates.isEmpty()) {
            for (TemplateRegion singlePlayerRegion : this.map.getSinglePlayerRegions()) {
                BlockPos pos = BlockPos.containing(singlePlayerRegion.getBounds().center());
                singlePlayerStates.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            for (TemplateRegion multiPlayerRegion : this.map.getMultiPlayerRegions()) {
                for (BlockPos pos : multiPlayerRegion.getBounds()) {
                    multiPlayerStates.put(pos, level.getBlockState(pos));
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        if (this.gameSpace.getPlayers().participants().size() - (playerLeaving ? 1 : 0) == 1) {
            singlePlayerStates.forEach((pos, state) -> level.setBlock(pos, state, 2));
            multiPlayerStates.forEach((pos, _state) -> level.setBlock(pos, Blocks.QUARTZ_BLOCK.defaultBlockState(), 2));
        } else {
            singlePlayerStates.forEach((pos, _state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
            multiPlayerStates.forEach((pos, _state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
        }
    }
    public void endGame(boolean failed) {
        if (failed) {
            gameOver = true;
            this.gameSpace.getPlayers().showTitle(Component.translatable("cooking_frenzy.game_over").withStyle(ChatFormatting.RED), 5 * SharedConstants.TICKS_PER_SECOND);
            this.scheduler.clearTasks();
            this.songBehaviour.stopSongs();
            this.gameSpace.getPlayers().playSound(SoundEvent.createFixedRangeEvent(CustomSounds.GAME_OVER, 1000), SoundSource.UI, 1, 1);
            this.scheduler.addTask(new Task(time + 5 * SharedConstants.TICKS_PER_SECOND, () -> this.gameSpace.close(GameCloseReason.FINISHED)));
        } else {
            this.gameSpace.getPlayers().sendPacket(new ClientboundStopSoundPacket(CustomSounds.DRUMROLL, SoundSource.UI));
            this.gameSpace.getPlayers().showTitle(Component.translatable("cooking_frenzy.active.day_end"), 5 * SharedConstants.TICKS_PER_SECOND);
            this.gameSpace.getPlayers().playSound(SoundEvent.createFixedRangeEvent(CustomSounds.TIMER_END, 1000), SoundSource.UI, 1, 1);
            this.scheduler.addTask(new Task(this.time + (5 * SharedConstants.TICKS_PER_SECOND), () -> {
                cleanUp();
                if (this.isTutorial) {
                    CookingFrenzyTutorial.Open(gameSpace, config, GameState.defaultGameState(), playerIntents);
                } else {
                    CookingFrenzySetup.Open(this.gameSpace, this.config, this.gameState, this.playerIntents);
                }
            }));
        }
    }
    private void cleanUp() {
        this.gameSpace.getLevels().remove(this.level);
        this.widgets.removeWidget(sidebar);
        this.widgets.removeWidget(timeBar);
        this.widgets.removeWidget(shopBar);
        this.widgets.close();
    }

    private void onTick() {
        this.updateWidgets();
        if (this.gameState.money() < this.minMoney && !gameOver) {
            endGame(true);
        }
        time++;
        if (this.itemThrownTime.containsValue(time)) {
            ArrayList<ItemEntity> itemsToDelete = new ArrayList<>();
            this.itemThrownTime.forEach((item, despawnTime) -> {
                if (time >= despawnTime) {
                    itemsToDelete.add(item);
                }
            });
            for (ItemEntity item : itemsToDelete) {
                this.itemThrownTime.remove(item);
                item.remove(Entity.RemovalReason.KILLED);
            }
        }
        this.scheduler.onTick(time);
    }

    private EventResult inventoryChange(ServerPlayer player, int i, ContainerInput containerInput, int i1) {
        if (player.hasContainerOpen()) {
            if (i == -999) {
                return EventResult.PASS;
            }
            ItemStack item = player.containerMenu.getSlot(i).getItem();
            if (item.getItem().equals(Items.BARRIER)) {
                return EventResult.DENY;
            } else {
                return EventResult.PASS;
            }
        } else {
            if (i >= 36 && i <= 44) {
                if (i - 36 < this.gameState.currentModifiers().getModifier(GameModifiers.hotbarSlotsAllowed)) {
                    return EventResult.PASS;
                }
            }
        }
        return EventResult.DENY;
    }

    private EventResult onEntitySpawn(Entity entity) {
        if (entity instanceof ItemEntity item) {
            this.itemThrownTime.put(item,
                    time + (
                            Math.round((1.5 * SharedConstants.TICKS_PER_MINUTE) / this.gameState.currentModifiers().getModifier(GameModifiers.itemDespawnSpeedMultiplier))
                    )
            );
        }
        return EventResult.PASS;
    }

    private EventResult onEntityUse(ServerPlayer player, Entity entity, InteractionHand hand, EntityHitResult entityHitResult) {
        if (entity instanceof ItemFrame) {
            return EventResult.DENY;
        }
        return EventResult.PASS;
    }

    private EventResult onItemPickup(ServerPlayer serverPlayer, ItemEntity itemEntity, ItemStack itemStack) {
        this.itemThrownTime.remove(itemEntity);
        return EventResult.PASS;
    }

    private EventResult onItemThrow(ServerPlayer serverPlayer, int i, ItemStack itemStack) {
        if (itemStack.getItem().equals(Items.BARRIER)) {
            return EventResult.DENY;
        }

        return EventResult.PASS;
    }

    private EventResult onBlockPlace(ServerPlayer player, ServerLevel level, BlockPos blockPos, BlockState blockState, UseOnContext useOnContext) {
        if (blockState.getBlock().equals(Blocks.BARRIER) || blockState.getBlock().equals(Blocks.WOOL.green())) {
            return EventResult.DENY;
        } else {
            return EventResult.PASS;
        }
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        if (state.getBlock() instanceof ShelfBlock || state.getBlock() instanceof SignBlock) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private EventResult onDeath(ServerPlayer player, DamageSource damageSource) {
        gameSpace.getPlayers().sendMessage(damageSource.getLocalizedDeathMessage(player));
        ArrayList<ItemStack> itemsToRemove = new ArrayList<>();
        for (ItemStack itemStack : player.getInventory()) {
            if (itemStack.getItem().equals(Items.BARRIER)) {
                itemsToRemove.add(itemStack);
            }
        }
        for (ItemStack itemStack : itemsToRemove) {
            player.getInventory().removeItem(itemStack);
        }
        player.getInventory().dropAll();
        player.setGameMode(GameType.SPECTATOR);
        player.setHealth(20);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5);
        scheduler.addCountdown(new CountdownTask(time + SharedConstants.TICKS_PER_SECOND * 5, (timePassed -> {
            player.sendSystemMessage(Component.translatable("cooking_frenzy.respawn_timer", 5 - timePassed), true);
            if (timePassed >= 5) {
                map.spawnPlayer(level, player);
            }
        })));
        return EventResult.DENY;
    }

    private void onLeave(ServerPlayer player) {
        checkSingleplayerStatus(true);
    }

    private void onJoin(ServerPlayer player) {
        checkSingleplayerStatus(false);
    }

    public void phaseRules(GameActivity activity) {
        activity.allow(GameRuleType.HUNGER);
        activity.listen(BlockBreakEvent.EVENT, (ServerPlayer _player, ServerLevel level, BlockPos pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            if (FarmingBehaviour.isPlant(block) || block.equals(Blocks.MELON) || block.equals(Blocks.PUMPKIN)) {
                return EventResult.PASS;
            } else {
                return EventResult.DENY;
            }
        });
    }

    public static void Open(GameSpace gameSpace, CookingFrenzyConfig config, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        gameSpace.setActivity(activity -> new CookingFrenzyActive(gameSpace, config, activity, state, playerIntents, false, null));
    }
    public static void Open(GameSpace gameSpace, CookingFrenzyConfig config, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        Open(gameSpace, config, GameState.defaultGameState(), playerIntents);
    }
    public static void OpenWithTutorial(GameSpace gameSpace, CookingFrenzyConfig config, HashMap<ServerPlayer, JoinIntent> playerIntents, TutorialBehaviour.TutorialType tutorialType) {
        gameSpace.setActivity(activity -> new CookingFrenzyActive(gameSpace, config, activity, GameState.defaultGameState(), playerIntents, true, tutorialType));
    }
}
