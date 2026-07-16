package me.ellieis.cooking_frenzy.gamestate.upgrades;

import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.gamestate.upgrades.modifier.DecrementModifierDebuff;
import me.ellieis.cooking_frenzy.gamestate.upgrades.modifier.IncrementModifierUpgrade;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Upgrades {
    public static ArrayList<BaseUpgrade> upgrades = new ArrayList<>(List.of(
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.hotbar_slots"),
                    Component.translatable("cooking_frenzy.upgrades.hotbar_slots.desc", 1),
                    GameModifiers.hotbarSlotsAllowed,
                    1,
                    100
            ),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.crafter_speed"),
                    Component.translatable("cooking_frenzy.upgrades.crafter_speed.desc", 25),
                    GameModifiers.crafterSpeedMultiplier,
                    0.25f,
                    50),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.furnace_speed"),
                    Component.translatable("cooking_frenzy.upgrades.furnace_speed.desc", 25),
                    GameModifiers.furnaceSpeedMultiplier,
                    0.25f,
                    50),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.crop_growth"),
                    Component.translatable("cooking_frenzy.upgrades.crop_growth.desc", 20),
                    GameModifiers.cropGrowthSpeedMultiplier,
                    0.2f,
                    50),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.snowball_timer"),
                    Component.translatable("cooking_frenzy.upgrades.snowball_timer.desc", 10),
                    GameModifiers.snowballTimerMultiplier,
                    0.1f,
                    50),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.customer_waiting_anger_rate"),
                    Component.translatable("cooking_frenzy.upgrades.customer_waiting_anger_rate.desc", 10),
                    GameModifiers.customerWaitingAngerRateMultiplier,
                    0.1f,
                    50),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.customer_order_anger_rate"),
                    Component.translatable("cooking_frenzy.upgrades.customer_order_anger_rate.desc", 10),
                    GameModifiers.customerOrderAngerRateMultiplier,
                    0.1f,
                    50),
            new RecipeMakerUpgrade(100, RecipeMaker.RecipeMakerType.CRAFTER),
            new RecipeMakerUpgrade(100, RecipeMaker.RecipeMakerType.FURNACE),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.upgrades.shop_queue"),
                    Component.translatable("cooking_frenzy.upgrades.shop_queue.desc"),
                    GameModifiers.shopDeliveryQueue,
                    1,
                    75)
    ));

    public static ArrayList<BaseUpgrade> debuffs = new ArrayList<>(List.of(
            new DecrementModifierDebuff<>(Component.translatable("cooking_frenzy.debuffs.item_despawn_speed"),
                    Component.translatable("cooking_frenzy.debuffs.item_despawn_speed.desc"),
                    GameModifiers.itemDespawnSpeedMultiplier,
                    0.1f),
            new DecrementModifierDebuff<>(Component.translatable("cooking_frenzy.debuffs.customer_spawn_rate_multiplier"),
                    Component.translatable("cooking_frenzy.debuffs.customer_spawn_rate_multiplier.desc"),
                    GameModifiers.customerSpawnRateMultiplier,
                    0.1f),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.debuffs.pricier_upgrades"),
                    Component.translatable("cooking_frenzy.debuffs.pricier_upgrades.desc"),
                    GameModifiers.upgradePriceMultiplier,
                    0.1f, 0),
            new ShopIncreaseDebuff(),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.debuffs.target_hit_precision"),
                    Component.translatable("cooking_frenzy.debuffs.target_hit_precision.desc"),
                    GameModifiers.targetHitPrecision,
                    1, 0),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.debuffs.freezer_damage"),
                    Component.translatable("cooking_frenzy.debuffs.freezer_damage.desc"),
                    GameModifiers.freezerDamage,
                    1, 0),
            new IncrementModifierUpgrade<>(Component.translatable("cooking_frenzy.debuffs.shop_delivery_speed_increase"),
                    Component.translatable("cooking_frenzy.debuffs.shop_delivery_speed_increase.desc"),
                    GameModifiers.shopDeliverySpeedMultiplier, 0.2f, 0)
    ));
    public static BaseUpgrade getRandomUpgrade(GameState state) {
        ArrayList<BaseUpgrade> upgrades = new ArrayList<>(Upgrades.upgrades);
        upgrades.removeIf(upgrade -> !upgrade.isAvailable(state));
        Collections.shuffle(upgrades);
        BaseUpgrade upgrade = upgrades.getFirst();
        upgrade.price = Math.round(upgrade.price * state.currentModifiers().getModifier(GameModifiers.upgradePriceMultiplier));
        return upgrade;
    }

    public static BaseUpgrade getRandomDebuff(GameState state) {
        ArrayList<BaseUpgrade> debuffs = new ArrayList<>(Upgrades.debuffs);
        debuffs.removeIf(debuff -> !debuff.isAvailable(state));
        Collections.shuffle(debuffs);
        return debuffs.getFirst();
    }
}
