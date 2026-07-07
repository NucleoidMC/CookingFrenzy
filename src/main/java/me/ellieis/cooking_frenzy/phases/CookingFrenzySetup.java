package me.ellieis.cooking_frenzy.phases;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.SongBehaviour;
import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.upgrades.BaseUpgrade;
import me.ellieis.cooking_frenzy.gamestate.upgrades.DebtUpgrade;
import me.ellieis.cooking_frenzy.gamestate.upgrades.Upgrades;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.map.Setup;
import me.ellieis.cooking_frenzy.scheduler.CountdownTask;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CookingFrenzySetup extends CookingFrenzyPhase<Setup> implements PhaseWithState {
    ArrayList<BaseUpgrade> availableUpgrades = new ArrayList<>();
    ArrayList<BaseUpgrade> availableDebuffs = new ArrayList<>();
    Scheduler scheduler;
    long time = 0;
    boolean pickedUpgrade = false;
    SidebarWidget sidebar;
    Display.TextDisplay display;
    int minMoney;
    private CookingFrenzySetup(GameSpace gameSpace, CookingFrenzyConfig config, GameActivity activity, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        Setup map = new Setup(gameSpace.getServer());
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig().setGenerator(map.asChunkGenerator());
        ServerLevel level = gameSpace.getLevels().add(levelConfig);
        super(config, activity, level, map, state);
        this.playerIntents = playerIntents;
        this.scheduler = new Scheduler(time);
        this.sidebar = GlobalWidgets.addTo(activity).addSidebar(Component.translatable("gameType.cooking_frenzy.cooking_frenzy"));
        updateSidebar();
        new SongBehaviour(gameSpace, activity, false, true);
        this.availableUpgrades.addAll(List.of(Upgrades.getRandomUpgrade(this.gameState), Upgrades.getRandomUpgrade(this.gameState), Upgrades.getRandomUpgrade(this.gameState)));
        this.availableDebuffs.addAll(List.of(Upgrades.getRandomDebuff(this.gameState), Upgrades.getRandomDebuff(this.gameState), Upgrades.getRandomDebuff(this.gameState)));
        this.display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        for (BaseUpgrade upgrade : this.gameState.upgrades()) {
            if (upgrade instanceof DebtUpgrade) {
                this.minMoney = -50;
                break;
            } else {
                this.minMoney = 0;
            }
        }
        display.setText(Component.translatable("cooking_frenzy.upgrades.title"));
        display.setBackgroundColor(0);
        Vec3 center = map.getUpgradeText().getBounds().center();
        display.teleportTo(level, center.x(), center.y(), center.z(), Set.of(), -90, 0, false);
        this.level.addFreshEntity(display);
        for (ServerPlayer player : gameSpace.getPlayers()) {
            player.getInventory().clearContent();
            ItemStack item = new ItemStack(Items.BARRIER);
            item.set(DataComponents.CUSTOM_NAME, Component.translatable("cooking_frenzy.setup.exit"));
            player.getInventory().setItem(8, item);
        }
    }

    public void phaseRules(GameActivity activity) {
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.BLOCK_DROPS);
        activity.listen(EntityDamageEvent.EVENT, (entity, source, amount) -> EventResult.DENY);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(ItemUseEvent.EVENT, this::onItemUse);
    }

    public GameState getState() {
        return this.gameState;
    }
    public void setState(GameState state) {
        this.gameState = state;
    }

    private void updateSidebar() {
        sidebar.set(lineBuider -> {
            lineBuider.add(Component.translatable("cooking_frenzy.sidebar.setup_phase").withStyle(ChatFormatting.YELLOW));
            lineBuider.add(Component.translatable("cooking_frenzy.sidebar.score", Component.literal("$" + gameState.money()).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.YELLOW));
            lineBuider.add(Component.translatable("cooking_frenzy.sidebar.day_counter", this.gameState.dayCount()).withStyle(ChatFormatting.YELLOW));
        });
    }
    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (this.level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LecternBlock && this.playerIntents.get(player) == JoinIntent.PLAY) {
            if (pickedUpgrade) {
                openDebuffGui(player);
            } else {
                openModifierGui(player);
            }
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem().equals(Items.BARRIER)) {
            this.gameSpace.getPlayers().kick(player);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private void onTick() {
        time++;
        scheduler.onTick(time);
    }

    private void openModifierGui(ServerPlayer player) {
        SimpleGui ui = new SimpleGui(MenuType.GENERIC_9x1, player, false);
        ui.setTitle(Component.translatable("cooking_frenzy.upgrades.title"));
        int i = 3;
        for (BaseUpgrade upgrade : this.availableUpgrades) {
            ItemStack item = new ItemStack(Items.PAPER);
            item.set(DataComponents.CUSTOM_NAME, upgrade.name);
            item.set(DataComponents.LORE, new ItemLore(List.of(upgrade.desc, Component.literal("$" + upgrade.price).withStyle(ChatFormatting.GREEN))));
            ui.setSlot(i, GuiElementBuilder.from(item).setCallback(() -> {
                if (this.gameState.money() - this.minMoney >= upgrade.price) {
                    this.pickedUpgrade = true;
                    this.gameState = this.gameState.decrementMoney(upgrade.price);
                    updateSidebar();
                    upgrade.onBuy(this);
                    for (ServerPlayer gameSpacePlayer : gameSpace.getPlayers()) {
                        if (gameSpacePlayer.hasContainerOpen()) {
                            gameSpacePlayer.closeContainer();
                            openDebuffGui(player);
                            display.setText(Component.translatable("cooking_frenzy.debuffs.title"));
                        }
                    }
                } else {
                    player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 1, 1, player.level().getSeed()));
                }
            }));
            i++;
        }
        ItemStack item = new ItemStack(Items.WOOL.green());
        item.set(DataComponents.CUSTOM_NAME, Component.translatable("cooking_frenzy.upgrades.next"));
        ui.setSlot(8, GuiElementBuilder.from(item).setCallback(() -> {
            this.pickedUpgrade = true;
            for (ServerPlayer gameSpacePlayer : gameSpace.getPlayers()) {
                if (gameSpacePlayer.hasContainerOpen()) {
                    gameSpacePlayer.closeContainer();
                    openDebuffGui(player);
                }
            }
        }));
        ui.open();
    }

    private void openDebuffGui(ServerPlayer player) {
        SimpleGui ui = new SimpleGui(MenuType.GENERIC_9x1, player, false);
        ui.setTitle(Component.translatable("cooking_frenzy.debuffs.title"));
        int i = 3;
        for (BaseUpgrade debuff : this.availableDebuffs) {
            ItemStack item = new ItemStack(Items.PAPER);
            item.set(DataComponents.CUSTOM_NAME, debuff.name);
            item.set(DataComponents.LORE, new ItemLore(List.of(debuff.desc)));
            ui.setSlot(i, GuiElementBuilder.from(item).setCallback(clickType -> {
                debuff.onBuy(this);
                for (ServerPlayer gameSpacePlayer : gameSpace.getPlayers()) {
                    if (gameSpacePlayer.hasContainerOpen()) {
                        gameSpacePlayer.closeContainer();
                    }
                }
                startCountdown();
            }));
            i++;
        }
        ui.open();
    }
    void startCountdown() {
        scheduler.addCountdown(new CountdownTask(time + (SharedConstants.TICKS_PER_SECOND * 5), (timePassed -> {
            this.gameSpace.getPlayers().sendActionBar(Component.translatable("cooking_frenzy.setup.day_starting", 5 - timePassed));
            if (timePassed >= 5) {
                this.gameSpace.getLevels().remove(this.level);
                if (gameState.dayCount() < 4) {
                    gameState = gameState.incrementTier();
                }
                for (ServerPlayer player : gameSpace.getPlayers()) {
                    player.getInventory().clearContent();
                }
                CookingFrenzyActive.Open(gameSpace, config, gameState.incrementDayCounter(), playerIntents);
            }
        })));
    }
    public static void Open(GameSpace gameSpace, CookingFrenzyConfig config, GameState state, HashMap<ServerPlayer, JoinIntent> playerIntents) {
        gameSpace.setActivity(activity -> new CookingFrenzySetup(gameSpace, config, activity, state, playerIntents));
    }
}
