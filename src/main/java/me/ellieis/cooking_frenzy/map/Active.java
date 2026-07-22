package me.ellieis.cooking_frenzy.map;

import me.ellieis.cooking_frenzy.CookingFrenzy;
import me.ellieis.cooking_frenzy.gamestate.Crafter;
import me.ellieis.cooking_frenzy.gamestate.Furnace;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.RecipeMaker;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.MapTemplateMetadata;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Active extends Map implements MapWithRecipeMaker, MapWithFreezer, MapWithCustomer {
    CookingFrenzyActive game;

    // recipe makers
    ArrayList<RecipeMaker> crafters = new ArrayList<>();
    ArrayList<RecipeMaker> furnaces = new ArrayList<>();

    // freezer
    List<TemplateRegion> meatProviders;
    List<TemplateRegion> freezerPlates;
    TemplateRegion foodDropper;
    TemplateRegion freezerDoor;
    TemplateRegion snowballContainer;
    TemplateRegion freezerArea;

    // customer
    List<TemplateRegion> customerSpawns;
    List<TemplateRegion> customerNodes;
    List<TemplateRegion> seats;
    TemplateRegion customerLights;

    // farming
    List<TemplateRegion> animalSpawners;
    TemplateRegion traderSpawn;
    TemplateRegion minecartSpawn;
    TemplateRegion farmingArea;
    TemplateRegion farmingPlate;
    TemplateRegion farmingBarrier;
    TemplateRegion farmingMinecartButton;
    TemplateRegion farmingButtonRedstoneBlock;
    TemplateRegion exitDetector;

    TemplateRegion shopDrop;
    List<TemplateRegion> lights;
    List<TemplateRegion> lightBreakerPlates;
    List<TemplateRegion> lightsArea;
    List<TemplateRegion> lightsBreaker;
    List<TemplateRegion> breakerDoors;
    List<TemplateRegion> singlePlayerRegions;
    List<TemplateRegion> multiPlayerRegions;
    GameModifiers modifiers;
    public HashMap<ServerPlayer, JoinIntent> playerIntents;
    public TutorialCameraPositions tutorialCameraPositions;
    public Active(MinecraftServer server, GameModifiers modifiers, HashMap<ServerPlayer, JoinIntent> playerIntents, boolean debugMode) {
        super(server, CookingFrenzy.identifier("main"));
        this.modifiers = modifiers;
        this.playerIntents = playerIntents;
        MapTemplateMetadata meta = this.template.getMetadata();

        // recipe makers
        meta.getRegions("crafter_area").forEach(region -> {
            boolean main = region.getData().getBooleanOr("main", false);
            crafters.add(new Crafter(main, main, false, region.getBounds().max(), FrontAndTop.valueOf(region.getData().getString("direction").orElse("EAST_UP").toUpperCase()),0, modifiers.getModifier(GameModifiers.crafterSpeedMultiplier), debugMode));
        });

        meta.getRegions("furnace_area").forEach(region -> {
            boolean main = region.getData().getBooleanOr("main", false);
            furnaces.add(new Furnace(main, main, false, region.getBounds().max(), FrontAndTop.valueOf(region.getData().getString("direction").orElse("EAST_UP").toUpperCase()), 0, modifiers.getModifier(GameModifiers.furnaceSpeedMultiplier), debugMode));
        });

        // freezer
        this.meatProviders = meta.getRegions("meat_provider").toList();
        this.foodDropper = meta.getFirstRegion("food_dropper");
        this.freezerDoor = meta.getFirstRegion("freezer_door");
        this.freezerPlates = meta.getRegions("freezer_plate").toList();
        this.snowballContainer = meta.getFirstRegion("snowball_container");
        this.freezerArea = meta.getFirstRegion("freezer_area");

        // customer
        this.customerSpawns = meta.getRegions("customer_spawn").toList();
        this.customerNodes = meta.getRegions("customer_node").toList();
        this.seats = meta.getRegions("seat").toList();
        this.customerLights = meta.getFirstRegion("customer_light");

        // farming
        this.animalSpawners = meta.getRegions("animal_spawn").toList();
        this.traderSpawn = meta.getFirstRegion("trader_spawn");
        this.minecartSpawn = meta.getFirstRegion("minecart_spawn");
        this.farmingArea = meta.getFirstRegion("farming_area");
        this.farmingPlate = meta.getFirstRegion("farming_plate");
        this.farmingBarrier = meta.getFirstRegion("farming_barrier");
        this.farmingMinecartButton = meta.getFirstRegion("farming_minecart_button");
        this.farmingButtonRedstoneBlock = meta.getFirstRegion("farming_button_redstone_block");
        this.exitDetector = meta.getFirstRegion("farming_exit_detector");

        this.shopDrop = meta.getFirstRegion("shop_drop");
        this.lights = meta.getRegions("light").toList();
        this.lightBreakerPlates = meta.getRegions("light_breaker_plate").toList();
        this.lightsArea = meta.getRegions("lights_area").toList();
        this.lightsBreaker = meta.getRegions("lights_breaker").toList();
        this.breakerDoors = meta.getRegions("breaker_door").toList();
        this.singlePlayerRegions = meta.getRegions("single_player").toList();
        this.multiPlayerRegions = meta.getRegions("multi_player").toList();
        this.tutorialCameraPositions = new TutorialCameraPositions(posFromMarker("customer_camera_angle"),
                posFromMarker("crafter_camera_angle"),
                posFromMarker("furnace_camera_angle"),
                posFromMarker("shop_camera_angle"),
                posFromMarker("freezer_outside_camera_angle"),
                posFromMarker("freezer_inside_camera_angle"),
                posFromMarker("freezer_target_camera_angle"),
                posFromMarker("farming_outside_camera_angle"),
                posFromMarker("trader_camera_angle"));
    }

    CamPos posFromMarker(String marker) {
        return new CamPos(Objects.requireNonNull(this.template.getMetadata().getFirstRegion(marker)));
    }

    public void setGame(CookingFrenzyActive game) {
        this.game = game;
    }

    public void unlockMainRecipeMakers(ServerLevel level) {
        for (RecipeMaker crafter : crafters) {
            if (crafter.isMain()) {
                crafter.unlock(level);
                break;
            }
        }
        for (RecipeMaker furnace : furnaces) {
            if (furnace.isMain()) {
                furnace.unlock(level);
                break;
            }
        }
    }

    public void unlockRecipeMaker(ServerLevel level, RecipeMaker.RecipeMakerType type) {
        ArrayList<RecipeMaker> makerList;
        if (type == RecipeMaker.RecipeMakerType.CRAFTER) {
            makerList = crafters;
        } else {
            makerList = furnaces;
        }
        for (RecipeMaker maker : makerList) {
            if (!maker.isUnlocked()) {
                maker.unlock(level);
                return;
            }
        }
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

    // farming
    public List<TemplateRegion> getAnimalSpawners() { return this.animalSpawners; }
    public TemplateRegion getTraderSpawn() { return this.traderSpawn; }
    public TemplateRegion getMinecartSpawn() { return this.minecartSpawn; }
    public TemplateRegion getFarmingArea() { return this.farmingArea; }
    public TemplateRegion getFarmingPlate() { return this.farmingPlate; }
    public TemplateRegion getFarmingBarrier() { return this.farmingBarrier; }
    public TemplateRegion getExitDetector() { return this.exitDetector; }
    public TemplateRegion getFarmingMinecartButton() { return this.farmingMinecartButton; }
    public TemplateRegion getFarmingButtonRedstoneBlock() { return this.farmingButtonRedstoneBlock; }
    public boolean isInFarmingArea(ServerPlayer player) { return this.farmingArea.getBounds().contains(player.blockPosition()); }

    public TemplateRegion getShopDrop() { return this.shopDrop; }
    public List<TemplateRegion> getLights() { return this.lights; }
    public List<TemplateRegion> getLightBreakerPlates() { return this.lightBreakerPlates; }
    public List<TemplateRegion> getLightsArea() { return this.lightsArea; }
    public List<TemplateRegion> getLightsBreaker() { return this.lightsBreaker; }
    public List<TemplateRegion> getBreakerDoors() { return this.breakerDoors; }
    public List<TemplateRegion> getSinglePlayerRegions() { return this.singlePlayerRegions; }
    public List<TemplateRegion> getMultiPlayerRegions() { return this.multiPlayerRegions; }

    @Override
    public void postSpawn(ServerLevel level, ServerPlayer player, TemplateRegion spawn) {
        if (this.playerIntents.get(player) == JoinIntent.PLAY) {
            player.setGameMode(GameType.SURVIVAL);
            Inventory inventory = player.getInventory();
            for (int i = this.modifiers.getModifier(GameModifiers.hotbarSlotsAllowed); i <= 35; i++) {
                ItemStack barrier = new ItemStack(Items.BARRIER);
                inventory.add(i, barrier);
            }
            player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5);
        } else {
            player.setGameMode(GameType.SPECTATOR);
        }
    }
    public record TutorialCameraPositions(CamPos customer, CamPos crafter, CamPos furnace, CamPos shop, CamPos freezerOutside, CamPos freezerInside, CamPos freezerTarget, CamPos farmingOutside, CamPos farmingTrader) {

    }
    public record CamPos(Vec3 pos, float pitch, float yaw) {
        public CamPos(TemplateRegion region) {
            this(region.getBounds().center(), region.getData().getFloatOr("pitch", 0), region.getData().getFloatOr("yaw", 0));
        }
    }
}
