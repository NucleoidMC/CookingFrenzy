package me.ellieis.cooking_frenzy.map;

import me.ellieis.cooking_frenzy.CookingFrenzy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.map_templates.TemplateRegion;

public class Setup extends Map {
    TemplateRegion upgradeText;
    public Setup(MinecraftServer server) {
        super(server, CookingFrenzy.identifier("setup"));
        this.upgradeText = this.template.getMetadata().getFirstRegion("upgrade_text");
    }

    public TemplateRegion getUpgradeText() {
        return this.upgradeText;
    }

    @Override
    public void postSpawn(ServerLevel level, ServerPlayer player, TemplateRegion spawn) {
        player.setGameMode(GameType.ADVENTURE);
    }
}
