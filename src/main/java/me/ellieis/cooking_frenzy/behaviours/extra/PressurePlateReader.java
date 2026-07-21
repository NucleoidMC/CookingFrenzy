package me.ellieis.cooking_frenzy.behaviours.extra;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import xyz.nucleoid.map_templates.TemplateRegion;

import java.util.List;

public class PressurePlateReader {
    public static int getPoweredPlates(List<TemplateRegion> regions, ServerLevel level) {
        int poweredPlates = 0;
        for (TemplateRegion plate : regions) {
            if (level.getBlockState(BlockPos.containing(plate.getBounds().center())).getValue(PressurePlateBlock.POWERED)) {
                poweredPlates++;
            }
        }
        return poweredPlates;
    }
    public static void updateSigns(List<TemplateRegion> regions, ServerLevel level, String top, String bottom, boolean isDisabled) {
        int val = getPoweredPlates(regions, level);
        Component[] text = {
                Component.translatable(top),
                Component.translatable(bottom, val, regions.size()),
                Component.empty(),
                Component.empty()
        };
        Component[] filteredText = {
                Component.empty(),
                Component.empty(),
                Component.empty(),
                Component.empty()
        };
        for (TemplateRegion region : regions) {
            if (level.getBlockEntity(BlockPos.containing(region.getBounds().center()).above()) instanceof SignBlockEntity sign) {
                sign.setText(new SignText(text, filteredText, (isDisabled) ? DyeColor.RED : DyeColor.WHITE, true), true);
            }
        }
    }
}
