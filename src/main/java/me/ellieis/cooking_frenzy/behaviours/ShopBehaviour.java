package me.ellieis.cooking_frenzy.behaviours;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.extra.ShopItem;
import me.ellieis.cooking_frenzy.events.ItemBuyEvent;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.upgrades.BaseUpgrade;
import me.ellieis.cooking_frenzy.gamestate.upgrades.ShopIncreaseDebuff;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ShopBehaviour extends BaseBehaviour {
    CookingFrenzyActive game;
    ServerLevel level;
    public static ArrayList<ArrayList<ShopItem>> items = new ArrayList<>(List.of(
            new ArrayList<>(List.of(
                    new ShopItem(5, Items.GLASS.getDefaultInstance()),
                    new ShopItem(5, Items.BROWN_MUSHROOM.getDefaultInstance()),
                    new ShopItem(5, Items.RED_MUSHROOM.getDefaultInstance()))),
           new ArrayList<>(List.of(
                   new ShopItem(10, Items.OAK_LOG.getDefaultInstance()),
                   new ShopItem(10, Items.RAW_IRON.getDefaultInstance()),
                   new ShopItem(15, Items.RAW_GOLD.getDefaultInstance())))
    ));
    ArrayList<QueuedItem> itemQueue = new ArrayList<>();
    Vec3 dropPos;
    long itemDelay;
    public ShopBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game) {
        super(gameSpace, activity, game.debugMode);
        this.game = game;
        this.level = game.level;
        this.dropPos = game.map.getShopDrop().getBounds().center();
        this.itemDelay = Math.round(20 * game.gameState.currentModifiers().getModifier(GameModifiers.shopDeliverySpeedMultiplier) * SharedConstants.TICKS_PER_SECOND);
        for (BaseUpgrade upgrade : game.gameState.upgrades()) {
            if (upgrade instanceof ShopIncreaseDebuff) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                ArrayList<ShopItem> row = items.get(random.nextInt(0, 2));
                int index = random.nextInt(0, row.size());
                ShopItem item = row.get(index);
                row.set(index, new ShopItem(Math.round(item.price() * 1.25f), item.item()));
            }
        }
    }

    public ArrayList<QueuedItem> getShopQueue() {
        return this.itemQueue;
    }

    private void buyItem(ShopItem item, ServerPlayer player) {
        if (item.price() <= game.gameState.money() - game.minMoney) {
            if (this.itemQueue.size() < this.game.gameState.currentModifiers().getModifier(GameModifiers.shopDeliveryQueue)) {
                game.gameState = game.gameState.decrementMoney(item.price());
                Item itemStack = item.item().getItem();
                long delay;
                if (itemStack.equals(Items.RAW_GOLD) || itemStack.equals(Items.RAW_IRON)) {
                    delay = itemDelay / 2;
                } else {
                    delay = itemDelay;
                }
                itemQueue.add(new QueuedItem(game.time + delay, game.time, item.item()));
                Stimuli.select().forEntity(player).get(ItemBuyEvent.EVENT).onItemBought(itemStack.getDefaultInstance());
            } else {
                player.sendSystemMessage(Component.translatable("cooking_frenzy.shop.queue_full").withStyle(ChatFormatting.RED), false);
                player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 2, 1, player.level().getSeed()));
            }

        } else {
            player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 2, 1, player.level().getSeed()));
        }
    }

    public void openShop(ServerPlayer player) {
        if (player.gameMode() != GameType.SURVIVAL) {
            return;
        }
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x4, player, false);
        gui.setTitle(Component.translatable("cooking_frenzy.shop"));
        int baseIndex;
        for (int i = 1; i < 3; i++) {
            baseIndex = i * 9;
            baseIndex += 3;
            for (ShopItem shopItem : items.get(i - 1)) {
                gui.setSlot(baseIndex, GuiElementBuilder.from(shopItem.item()).setCallback(() -> {
                    buyItem(shopItem, player);
                }));
                baseIndex++;
            }
        }
        gui.open();
    }

    public static void openStaticShop(ServerPlayer player) {
        // this is for lobby
        if (player.gameMode() != GameType.SURVIVAL) {
            return;
        }
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x4, player, false);
        gui.setTitle(Component.translatable("cooking_frenzy.shop"));
        int baseIndex;
        for (int i = 1; i < 3; i++) {
            baseIndex = i * 9;
            baseIndex += 3;
            for (ShopItem shopItem : items.get(i - 1)) {
                gui.setSlot(baseIndex, GuiElementBuilder.from(shopItem.item()));
                baseIndex++;
            }
        }
        gui.open();
    }

    @Override
    void setupEvents() {
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void onTick() {
        for (QueuedItem queuedItem : itemQueue) {
            if (queuedItem.time() <= game.time) {
                this.level.addFreshEntity(new ItemEntity(this.level, dropPos.x(), dropPos.y(), dropPos.z(), queuedItem.item.copy(), 0, 0, 0));
                this.level.playSound(null, dropPos.x(), dropPos.y(), dropPos.z(), SoundEvents.CHICKEN_EGG, SoundSource.UI);
            }
        }
        itemQueue.removeIf((item -> item.time() <= game.time));
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LecternBlock) {
            openShop(player);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public record QueuedItem(long time, long startedTime, ItemStack item) {

    }
}
