package me.ellieis.cooking_frenzy.map;

import me.ellieis.cooking_frenzy.CookingFrenzy;
import me.ellieis.cooking_frenzy.gamestate.Crafter;
import me.ellieis.cooking_frenzy.gamestate.Furnace;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.map_templates.MapTemplateMetadata;
import xyz.nucleoid.map_templates.TemplateRegion;

import java.util.ArrayList;
import java.util.List;

public class Lobby extends Map implements MapWithRecipeMaker, MapWithFreezer, MapWithCustomer {
    public MapTemplateMetadata data;

    // customer
    List<TemplateRegion> customerSpawns;
    List<TemplateRegion> customerNodes;
    List<TemplateRegion> seats;
    TemplateRegion customerLights;

    // freezer
    List<TemplateRegion> meatProviders;
    List<TemplateRegion> freezerPlates;
    TemplateRegion foodDropper;
    TemplateRegion freezerDoor;
    TemplateRegion snowballContainer;
    TemplateRegion freezerArea;

    // recipe makers
    ArrayList<RecipeMaker> crafters = new ArrayList<>();
    ArrayList<RecipeMaker> furnaces = new ArrayList<>();
    public Lobby(MinecraftServer server, GameModifiers modifiers, boolean debugMode) {
        super(server, CookingFrenzy.identifier("lobby"));
        this.data = this.template.getMetadata();
        // recipe makers
        data.getRegions("crafter_area").forEach(region -> {
            boolean main = region.getData().getBooleanOr("main", false);
            crafters.add(new Crafter(main, main, false, region.getBounds().max(), FrontAndTop.valueOf(region.getData().getString("direction").orElse("EAST_UP").toUpperCase()),0, modifiers.getModifier(GameModifiers.crafterSpeedMultiplier), debugMode));
        });

        data.getRegions("furnace_area").forEach(region -> {
            boolean main = region.getData().getBooleanOr("main", false);
            furnaces.add(new Furnace(main, main, false, region.getBounds().max(), FrontAndTop.valueOf(region.getData().getString("direction").orElse("EAST_UP").toUpperCase()), 0, modifiers.getModifier(GameModifiers.furnaceSpeedMultiplier), debugMode));
        });

        // freezer
        this.meatProviders = data.getRegions("meat_provider").toList();
        this.foodDropper = data.getFirstRegion("food_dropper");
        this.freezerDoor = data.getFirstRegion("freezer_door");
        this.freezerPlates = data.getRegions("freezer_plate").toList();
        this.snowballContainer = data.getFirstRegion("snowball_container");
        this.freezerArea = data.getFirstRegion("freezer_area");

        // customer
        this.customerSpawns = data.getRegions("customer_spawn").toList();
        this.customerNodes = data.getRegions("customer_node").toList();
        this.seats = data.getRegions("seat").toList();
        this.customerLights = data.getFirstRegion("customer_light");
    }

    public ArrayList<RecipeMaker> getRecipeMakers(RecipeMaker.RecipeMakerType type) {
        if (type == RecipeMaker.RecipeMakerType.CRAFTER) {
            return this.crafters;
        } else {
            return this.furnaces;
        }
    }

    // freezer
    public List<TemplateRegion> getMeatProviders() {
        return this.meatProviders;
    }
    public List<TemplateRegion> getFreezerPlates() {
        return this.freezerPlates;
    }
    public TemplateRegion getFoodDropper() {
        return this.foodDropper;
    }
    public TemplateRegion getFreezerDoor() {
        return this.freezerDoor;
    }
    public TemplateRegion getSnowballContainer() { return this.snowballContainer; }
    public TemplateRegion getFreezerArea() { return this.freezerArea; }

    // customer
    public List<TemplateRegion> getCustomerSpawns() { return this.customerSpawns; }
    public List<TemplateRegion> getCustomerNodes() { return this.customerNodes; }
    public List<TemplateRegion> getSeats() { return this.seats; }
    public TemplateRegion getCustomerLights() { return customerLights; }

    @Override
    public void postSpawn(ServerLevel level, ServerPlayer player, TemplateRegion spawn) {
        player.setGameMode(GameType.SURVIVAL);
    }
}
