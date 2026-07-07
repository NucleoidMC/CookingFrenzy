package me.ellieis.cooking_frenzy.mixins;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StemBlock.class)
public interface StemBlockAccessor {
    @Accessor("fruit")
    ResourceKey<Block> cooking_frenzy$getFruit();

    @Accessor("attachedStem")
    ResourceKey<Block> cooking_frenzy$getAttachedStem();

    @Accessor("fruitSupportBlocks")
    TagKey<Block> cooking_frenzy$getFruitSupportBlocks();
}
