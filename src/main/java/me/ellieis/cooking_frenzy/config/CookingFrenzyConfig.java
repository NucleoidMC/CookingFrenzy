package me.ellieis.cooking_frenzy.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

import java.util.OptionalInt;

public record CookingFrenzyConfig(WaitingLobbyConfig playerConfig, boolean debugMode) {
    public static final MapCodec<CookingFrenzyConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
                WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig(new PlayerLimiterConfig(OptionalInt.empty(), true), 1, 6, new WaitingLobbyConfig.Countdown(30, 5))).forGetter(CookingFrenzyConfig::playerConfig),
                Codec.BOOL.optionalFieldOf("debug_mode", true).forGetter(CookingFrenzyConfig::debugMode)
        ).apply(instance, CookingFrenzyConfig::new)
    );
}
