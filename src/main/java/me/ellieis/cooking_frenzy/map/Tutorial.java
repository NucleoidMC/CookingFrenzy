package me.ellieis.cooking_frenzy.map;

import me.ellieis.cooking_frenzy.CookingFrenzy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.map_templates.TemplateRegion;

public class Tutorial extends Map {
    public Tutorial(MinecraftServer server) {
        super(server, CookingFrenzy.identifier("tutorial"));
    }
    @Override
    public void postSpawn(ServerLevel level, ServerPlayer player, TemplateRegion spawn) {
        player.setGameMode(GameType.ADVENTURE);
        ItemStack itemStack = new ItemStack(Items.BED.red());
        itemStack.set(DataComponents.CUSTOM_NAME, Component.translatable("text.plasmid.game.waiting_lobby.leave_game"));
        player.getInventory().add(8, itemStack);
    }
}
