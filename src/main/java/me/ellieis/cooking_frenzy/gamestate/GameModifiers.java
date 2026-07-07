package me.ellieis.cooking_frenzy.gamestate;

import java.util.HashMap;
import java.util.Map;

public class GameModifiers {
    private final Map<ModifierType<?>, TypedModifier<?>> modifiers = new HashMap<>();
    public static ModifierType<Integer> hotbarSlotsAllowed = new ModifierType<>("hotbar_slots_allowed", Integer.class);
    public static ModifierType<Float> crafterSpeedMultiplier = new ModifierType<>("crafter_speed_multiplier", Float.class);
    public static ModifierType<Float> furnaceSpeedMultiplier = new ModifierType<>("furnace_speed_multiplier", Float.class);
    public static ModifierType<Float> cropGrowthSpeedMultiplier = new ModifierType<>("crop_growth_speed_multiplier", Float.class);
    public static ModifierType<Float> snowballTimerMultiplier = new ModifierType<>("snowball_timer_multiplier", Float.class);
    public static ModifierType<Float> customerSpawnRateMultiplier = new ModifierType<>("customer_spawn_rate_multiplier", Float.class);
    public static ModifierType<Float> customerWaitingAngerRateMultiplier = new ModifierType<>("customer_waiting_anger_rate_multiplier", Float.class);
    public static ModifierType<Float> customerOrderAngerRateMultiplier = new ModifierType<>("customer_order_anger_rate_multiplier", Float.class);
    public static ModifierType<Float> itemDespawnSpeedMultiplier = new ModifierType<>("item_despawn_speed_multiplier", Float.class);
    public static ModifierType<Float> upgradePriceMultiplier = new ModifierType<>("upgrade_price_multiplier", Float.class);
    public static ModifierType<Float> shopDeliverySpeedMultiplier = new ModifierType<>("shop_delivery_speed_multiplier", Float.class);
    public static ModifierType<Integer> shopDeliveryQueue = new ModifierType<>("shop_delivery_queue", Integer.class);
    public GameModifiers(int hotbarSlotsAllowed, float crafterSpeedMultiplier, float furnaceSpeedMultiplier, float cropGrowthSpeedMultiplier, float snowballTimeMultiplier, float customerSpawnRateMultiplier, float customerWaitingAngerRateMultiplier, float customerOrderAngerRateMultiplier, float itemDespawnSpeedMultiplier, float upgradePriceMultiplier, float shopDeliverySpeedMultiplier, int shopDeliveryQueue) {
        this.modifiers.put(GameModifiers.hotbarSlotsAllowed, new TypedModifier<>(hotbarSlotsAllowed));
        this.modifiers.put(GameModifiers.crafterSpeedMultiplier, new TypedModifier<>(crafterSpeedMultiplier));
        this.modifiers.put(GameModifiers.furnaceSpeedMultiplier, new TypedModifier<>(furnaceSpeedMultiplier));
        this.modifiers.put(GameModifiers.cropGrowthSpeedMultiplier, new TypedModifier<>(cropGrowthSpeedMultiplier));
        this.modifiers.put(GameModifiers.snowballTimerMultiplier, new TypedModifier<>(snowballTimeMultiplier));
        this.modifiers.put(GameModifiers.customerSpawnRateMultiplier, new TypedModifier<>(customerSpawnRateMultiplier));
        this.modifiers.put(GameModifiers.customerWaitingAngerRateMultiplier, new TypedModifier<>(customerWaitingAngerRateMultiplier));
        this.modifiers.put(GameModifiers.customerOrderAngerRateMultiplier, new TypedModifier<>(customerOrderAngerRateMultiplier));
        this.modifiers.put(GameModifiers.itemDespawnSpeedMultiplier, new TypedModifier<>(itemDespawnSpeedMultiplier));
        this.modifiers.put(GameModifiers.upgradePriceMultiplier, new TypedModifier<>(upgradePriceMultiplier));
        this.modifiers.put(GameModifiers.shopDeliverySpeedMultiplier, new TypedModifier<>(shopDeliverySpeedMultiplier));
        this.modifiers.put(GameModifiers.shopDeliveryQueue, new TypedModifier<>(shopDeliveryQueue));
    }

    public <T> void setModifier(ModifierType<T> type, T value) {
        TypedModifier<?> modifier = this.modifiers.get(type);
        if (modifier == null) {
            throw new IllegalArgumentException("Type " + type.toString() + " is not registered as a modifier");
        }
        TypedModifier<T> typedModifier = (TypedModifier<T>) modifier;
        typedModifier.set(value);
    }

    public <T> T getModifier(ModifierType<T> type) {
        TypedModifier<?> modifier = this.modifiers.get(type);
        if (modifier == null) {
            throw new IllegalArgumentException("Type " + type.toString() + " is not registered as a modifier");
        }
        TypedModifier<T> typedModifier = (TypedModifier<T>) modifier;
        return typedModifier.get();
    }

    public GameModifiers() {
        this(3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    }
}
