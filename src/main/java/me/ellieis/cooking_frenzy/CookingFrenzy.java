package me.ellieis.cooking_frenzy;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.cooking_frenzy.config.CookingFrenzyConfig;
import me.ellieis.cooking_frenzy.map.Map;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyLobby;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyPhase;
import me.ellieis.cooking_frenzy.textures.GuiTextures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameTypes;

import java.util.ArrayList;

public class CookingFrenzy implements ModInitializer {
    public static final String MOD_ID = "cooking_frenzy";
    public static final Logger LOGGER = LogManager.getLogger(CookingFrenzy.class);
    public static final ArrayList<CookingFrenzyPhase<Map>> games = new ArrayList<>();
    @Override
    public void onInitialize() {
        GameTypes.register(CookingFrenzy.identifier("cooking_frenzy"), CookingFrenzyConfig.CODEC, CookingFrenzyLobby::Open);
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        GuiTextures.register();
    }

    public static CookingFrenzyPhase<Map> getGameByLevel(ServerLevel level) {
        for (CookingFrenzyPhase<Map> game : games) {
            if (game.level.equals(level)) {
                return game;
            }
        }
        return null;
    }
    public static Identifier identifier(String value) {
        return Identifier.fromNamespaceAndPath(MOD_ID, value);
    }
}
