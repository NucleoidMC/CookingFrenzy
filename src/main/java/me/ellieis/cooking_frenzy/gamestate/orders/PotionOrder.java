package me.ellieis.cooking_frenzy.gamestate.orders;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

public record PotionOrder(PotionContents basePotion, int timeLimit, int tier, int money, Optional<Component> potionName) implements Order {
    public Item food() {
        return Items.POTION;
    }
    public Component name() {
        return potionName.orElse(basePotion.getName(""));
    }
    public ItemStack displayStack() {
        ItemStack item = Items.POTION.getDefaultInstance().copy();
        item.set(DataComponents.POTION_CONTENTS, basePotion);
        return item;
    }
    public boolean isCorrectOrder(ItemStack item) {
        if (item.has(DataComponents.POTION_CONTENTS)) {
            return item.get(DataComponents.POTION_CONTENTS).equals(basePotion);
        }
        return false;
    }
    public static PotionOrder inSeconds(PotionContents potion, int timeLimit, int tier) {
        return new PotionOrder(potion, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, 60, Optional.empty());
    }
    public static PotionOrder inSeconds(PotionContents potion, int timeLimit, int tier, int money) {
        return new PotionOrder(potion, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, money, Optional.empty());
    }
    public static PotionOrder inSeconds(Holder<Potion> potion, int timeLimit, int tier) {
        return new PotionOrder(new PotionContents(potion), timeLimit * SharedConstants.TICKS_PER_SECOND, tier, 60, Optional.empty());
    }
    public static PotionOrder inSeconds(Holder<Potion> potion, int timeLimit, int tier, int money) {
        return new PotionOrder(new PotionContents(potion), timeLimit * SharedConstants.TICKS_PER_SECOND, tier, money, Optional.empty());
    }
    public static PotionOrder inSeconds(Holder<Potion> potion, int timeLimit, int tier, Component potionName) {
        return new PotionOrder(new PotionContents(potion), timeLimit * SharedConstants.TICKS_PER_SECOND, tier, 60, Optional.of(potionName));
    }
    public static PotionOrder inSeconds(Holder<Potion> potion, int timeLimit, int tier, int money, Component potionName) {
        return new PotionOrder(new PotionContents(potion), timeLimit * SharedConstants.TICKS_PER_SECOND, tier, money, Optional.of(potionName));
    }
}
