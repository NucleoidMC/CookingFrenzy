package me.ellieis.cooking_frenzy.map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public abstract class Map {
    protected final List<TemplateRegion> spawns;
    protected final MapTemplate template;
    protected final MinecraftServer server;
    public Map(MinecraftServer server, Identifier templateId) {
        MapTemplate template;
        try {
            template = MapTemplateSerializer.loadFromResource(server, templateId);
        } catch(IOException exception) {
            throw new GameOpenException(Component.literal("Failed to load map " + templateId), exception);
        }
        this.server = server;
        this.spawns = template.getMetadata().getRegions("spawn").toList();
        this.template = template;
    }

    public void spawnPlayer(ServerLevel level, ServerPlayer player) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3 pos = spawn.getBounds().center();
        player.teleportTo(level, pos.x(), pos.y(), pos.z(), new HashSet<>(), spawn.getData().getFloatOr("Rotation", 0f), 0, true);
        player.setOnGround(true);
        player.setDeltaMovement(0, 0, 0);
        player.setGameMode(GameType.ADVENTURE);
        this.postSpawn(level, player, spawn);
    }

    public abstract void postSpawn(ServerLevel level, ServerPlayer player, TemplateRegion spawn);

    public ChunkGenerator asChunkGenerator() {
        return new TemplateChunkGenerator(this.server, this.template);
    }
}
