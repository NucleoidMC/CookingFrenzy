package me.ellieis.cooking_frenzy.gamestate.orders;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Orders {
    // time limit is in seconds, not ticks
    public static ArrayList<BaseOrder> TIER1 = new ArrayList<>(List.of(
            BaseOrder.inSeconds(Items.COOKED_PORKCHOP, 150, 1),
            BaseOrder.inSeconds(Items.COOKED_CHICKEN, 150, 1),
            BaseOrder.inSeconds(Items.BREAD, 180, 1, 90),
            BaseOrder.inSeconds(Items.BAKED_POTATO, 150, 1),
            BaseOrder.inSeconds(Items.CARROT, 120, 1),
            BaseOrder.inSeconds(Items.BEETROOT, 120, 1),
            BaseOrder.inSeconds(Items.POTION, 180, 1),
            BaseOrder.inSeconds(Items.EGG, 100, 1)
    ));
    public static ArrayList<BaseOrder> TIER2 = new ArrayList<>(List.of(
            BaseOrder.inSeconds(Items.MELON_SLICE, 180, 2),
            BaseOrder.inSeconds(Items.GOLDEN_CARROT, 240, 2),
            BaseOrder.inSeconds(Items.MILK_BUCKET, 210, 2),
            BaseOrder.inSeconds(Items.MUSHROOM_STEW, 180, 2),
            BaseOrder.inSeconds(Items.PUMPKIN_PIE, 180, 2),
            BaseOrder.inSeconds(Items.COOKIE, 180, 2),
            BaseOrder.inSeconds(Items.COOKED_BEEF, 150, 2)
            ));
    public static ArrayList<BaseOrder> TIER3 = new ArrayList<>(List.of(
            BaseOrder.inSeconds(Items.CAKE, 270, 3, 200),
            BaseOrder.inSeconds(Items.BEETROOT_SOUP, 180, 3, 90)
            ));
    public static BaseOrder random(int tier, RandomSource random) {
        int tier1Chance = 0;
        int tier2Chance = 0;
        int tier3Chance = 0;
        if (tier == 1) {
            tier1Chance = 100;
        } else if (tier == 2) {
            tier1Chance = 75;
            tier2Chance = 25;
        } else if (tier == 3) {
            tier1Chance = 50;
            tier2Chance = 40;
            tier3Chance = 10;
        } else if (tier >= 4) {
            tier1Chance = 30;
            tier2Chance = 50;
            tier3Chance = 20;
        }

        WeightedList.Builder<BaseOrder> builder = new WeightedList.Builder<BaseOrder>();
        for (BaseOrder baseOrder : TIER1) {
            builder.add(baseOrder, tier1Chance);
        }
        for (BaseOrder baseOrder : TIER2) {
            builder.add(baseOrder, tier2Chance);
        }
        for (BaseOrder baseOrder : TIER3) {
            builder.add(baseOrder, tier3Chance);
        }

        return builder.build().getRandomOrThrow(random);
    }
}
