package me.ellieis.cooking_frenzy.behaviours.extra;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.textures.GuiTextures;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

public class Farmer {
    ArrayList<ArrayList<ShopItem>> shopItems;
    CookingFrenzyActive game;
    public Farmer(CookingFrenzyActive game) {
        this.game = game;
        ItemStack hoe = Items.WOODEN_HOE.getDefaultInstance();
        hoe.setDamageValue(hoe.getMaxDamage() - 2);
        this.shopItems = new ArrayList<>(List.of(
                new ArrayList<>(List.of(
                        new ShopItem(30, Items.WHEAT_SEEDS.getDefaultInstance()),
                        new ShopItem(30, Items.CARROT.getDefaultInstance()),
                        new ShopItem(30, Items.POTATO.getDefaultInstance())
                )),
                new ArrayList<>(List.of(
                        new ShopItem(30, Items.BEETROOT_SEEDS.getDefaultInstance()),
                        new ShopItem(30, Items.MELON_SEEDS.getDefaultInstance()),
                        new ShopItem(30, Items.PUMPKIN_SEEDS.getDefaultInstance())
                )),
                new ArrayList<>(List.of(
                        ShopItem.EMPTY(),
                        new ShopItem(30, Items.COCOA_BEANS.getDefaultInstance()),
                        ShopItem.EMPTY()
                )),
                new ArrayList<>(),
                new ArrayList<>(List.of(
                        new ShopItem(5, Items.SUGAR_CANE.getDefaultInstance()),
                        new ShopItem(5, hoe),
                        new ShopItem(5, Items.BONE_MEAL.getDefaultInstance())
                ))
        )
        );
    }
    public void openUiForPlayer(ServerPlayer player) {
        if (player.gameMode() != GameType.SURVIVAL) {
            return;
        }
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
        gui.setTitle(GuiTextures.FARMER_SHOP.apply(Component.empty()));
        for (int i = 0; i < 5; i++) {
            int baseIndex = i * 9;
            baseIndex += 3;
            for (ShopItem item : shopItems.get(i)) {
                if (item.price() == -1) {
                    baseIndex++;
                    continue;
                }
                gui.setSlot(baseIndex, GuiElementBuilder.from(item.item()).setCallback(() -> {
                    buyItem(player, item);
                }));
                baseIndex++;
            }
        }
        player.playSound(SoundEvents.VILLAGER_AMBIENT);
        gui.open();
    }

    public void buyItem(ServerPlayer player, ShopItem item) {
        if (game.gameState.money() - game.minMoney >= item.price()) {
            game.gameState = game.gameState.decrementMoney(item.price());
            ItemStack itemStack = item.item().copy();
            if (!player.getInventory().add(itemStack)) {
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack));
            }
        } else {
            player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 1, 1, player.level().getSeed()));
        }
    }
}
