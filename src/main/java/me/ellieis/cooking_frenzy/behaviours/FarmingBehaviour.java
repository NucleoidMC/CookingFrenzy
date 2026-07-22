package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.behaviours.extra.Farmer;
import me.ellieis.cooking_frenzy.behaviours.extra.PressurePlateReader;
import me.ellieis.cooking_frenzy.behaviours.malfunctions.MalfunctionType;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.mixins.StemBlockAccessor;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.DroppedItemsResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FarmingBehaviour extends DisableableBehaviour {
    ServerLevel level;
    Mannequin trader;
    BlockPos farmingPlatePos;
    BlockPos exitDetectorPos;
    TemplateRegion farmingBarrier;
    boolean isEntranceOpen = false;
    boolean debounce = true;
    Scheduler scheduler;
    Farmer farmer;
    boolean isMinecartGlowing = false;
    boolean isFarmerGlowing = false;
    Minecart minecart;
    ArrayList<PlantInfo> plants = new ArrayList<>();
    public int cropGrowTime;
    CookingFrenzyActive game;
    BlockPos singlePlayerButtonPos;
    BlockPos farmingButtonRedstoneBlock;
    ArrayList<Item> seeds = new ArrayList<>(List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.POTATO, Items.CARROT, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS));
    public FarmingBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game) {
        super(gameSpace, activity, game.debugMode, List.of(MalfunctionType.LIGHTS));
        this.level = game.level;
        this.farmingPlatePos = BlockPos.containing(game.map.getFarmingPlate().getBounds().center());
        this.exitDetectorPos = BlockPos.containing(game.map.getExitDetector().getBounds().center());
        this.farmingBarrier = game.map.getFarmingBarrier();
        this.scheduler = game.scheduler;
        this.farmer = new Farmer(game);
        this.game = game;
        this.farmingButtonRedstoneBlock = BlockPos.containing(game.map.getFarmingButtonRedstoneBlock().getBounds().center());
        this.cropGrowTime = Math.round(SharedConstants.TICKS_PER_MINUTE / game.gameState.currentModifiers().getModifier(GameModifiers.cropGrowthSpeedMultiplier));
        for (TemplateRegion region : this.game.map.getSinglePlayerRegions()) {
            if (region.getData().getBooleanOr("farming", false)) {
                this.singlePlayerButtonPos = BlockPos.containing(region.getBounds().center());
            }
        }
        if (game.debugMode) {
            System.out.println("Crop growth time: " + cropGrowTime);
        }
        for (TemplateRegion animalSpawner : game.map.getAnimalSpawners()) {
            EntityType<?> entityType = null;
            String type = animalSpawner.getData().getString("type").orElseThrow();
            if (type.equals("cow")) {
                entityType = EntityTypes.COW;
            } else if (type.equals("chicken")) {
                entityType = EntityTypes.CHICKEN;
            }
            if (entityType == null) return;

            for (int i = 0; i < 3; i++) {
                LivingEntity animal = null;
                if (entityType == EntityTypes.CHICKEN) {
                    animal = new Chicken((EntityType<? extends Chicken>) entityType, level);
                } else {
                    animal = new Cow((EntityType<? extends Cow>) entityType, level);
                }
                Vec3 pos = animalSpawner.getBounds().center();
                animal.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), 0, 0, false);
                level.addFreshEntity(animal);
            }
        }

        this.trader = new Mannequin(EntityTypes.MANNEQUIN, level);
        Vec3 traderPos = game.map.getTraderSpawn().getBounds().center();
        trader.teleportTo(level, traderPos.x(), traderPos.y(), traderPos.z(), Set.of(), 0, 0, false);

        Vec3 minecartPos = game.map.getMinecartSpawn().getBounds().center();
        Minecart minecart = new Minecart(EntityTypes.MINECART, level);
        this.minecart = minecart;
        minecart.teleportTo(level, minecartPos.x(), minecartPos.y(), minecartPos.z(), Set.of(), 0, 0, false);

        level.addFreshEntity(trader);
        level.addFreshEntity(minecart);
    }

    protected void setupEvents() {
        activity.listen(EntityUseEvent.EVENT, this::onEntityUse);
        activity.listen(BlockPlaceEvent.AFTER, this::onBlockPlace);
        activity.listen(BlockBreakEvent.EVENT, this::onBlockBreak);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(BlockDropItemsEvent.EVENT, this::droppedItemModifier);
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    public void glowFarmer(boolean isGlowing) {
        this.isFarmerGlowing = isGlowing;
    }

    public void glowMinecart(boolean isGlowing) {
        this.isMinecartGlowing = isGlowing;
    }

    private boolean isSeed(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item.equals(Items.WHEAT_SEEDS) ||
                item.equals(Items.CARROT) ||
                item.equals(Items.POTATO) ||
                item.equals(Items.SUGAR_CANE) ||
                item.equals(Items.BEETROOT_SEEDS) ||
                item.equals(Items.PUMPKIN_SEEDS) ||
                item.equals(Items.COCOA_BEANS) ||
                item.equals(Items.MELON_SEEDS);
    }

    public static boolean isPlant(Block block) {
        return isCrop(block) || block instanceof SugarCaneBlock || block instanceof StemBlock || block instanceof CocoaBlock;
    }

    public static boolean isCrop(Block block) {
        return block instanceof CropBlock;
    }

    private PlantInfo getPlantByPos(BlockPos pos) {
        for (PlantInfo plant : plants) {
            if (plant.pos().equals(pos)) {
                return plant;
            }
        }
        return null;
    }
    private void onTick() {
        Vec3 traderPos = this.trader.position();
        List<Player> nearbyPlayers = this.level.getNearbyEntities(Player.class, TargetingConditions.forNonCombat(), this.trader, AABB.unitCubeFromLowerCorner(traderPos).inflate(10));
        Player nearestPlayer = null;
        for (Player player : nearbyPlayers) {
            if (nearestPlayer == null) {
                nearestPlayer = player;
                continue;
            }
            if (player.position().distanceTo(traderPos) < nearestPlayer.position().distanceTo(traderPos)) {
                nearestPlayer = player;
            }
        }
        if (nearestPlayer != null) {
            this.trader.lookAt(EntityAnchorArgument.Anchor.EYES, nearestPlayer.getEyePosition());
        }
        boolean powered = this.level.getBlockState(farmingPlatePos).getValue(PressurePlateBlock.POWERED);
        if (debounce && powered && !this.isEntranceOpen) {
            if (!this.isDisabled) {
                for (BlockPos bound : this.farmingBarrier.getBounds()) {
                    this.level.setBlock(bound, Blocks.AIR.defaultBlockState(), 2);
                }
                this.level.setBlockAndUpdate(BlockPos.containing(this.farmingBarrier.getBounds().centerBottom()), Blocks.RAIL.defaultBlockState());
                this.isEntranceOpen = true;
            }
        } else if (debounce && !powered && this.isEntranceOpen) {
            for (BlockPos bound : this.farmingBarrier.getBounds()) {
                this.level.setBlock(bound, Blocks.BARRIER.defaultBlockState(), 2);
            }
            this.isEntranceOpen = false;
        }
        if (this.isDisabled) {
            PressurePlateReader.updateSigns(List.of(game.map.getFarmingPlate()), level, "cooking_frenzy.farming.disabled", "cooking_frenzy.farming.disabled.2", true);
        } else {
            PressurePlateReader.updateSigns(List.of(game.map.getFarmingPlate()), level, "cooking_frenzy.farming.sign.top", "cooking_frenzy.freezer.sign.bottom", false);
        }

        powered = this.level.getBlockState(this.exitDetectorPos).getValue(PressurePlateBlock.POWERED);
        BlockState button = this.level.getBlockState(this.singlePlayerButtonPos);
        if (button.getBlock() != Blocks.AIR) {
            powered = powered || button.getValue(BlockStateProperties.POWERED);
        }
        if (debounce && powered && !this.isEntranceOpen) {
            for (BlockPos bound : this.farmingBarrier.getBounds()) {
                this.level.setBlock(bound, Blocks.AIR.defaultBlockState(), 2);
            }
            this.level.setBlockAndUpdate(this.farmingButtonRedstoneBlock, Blocks.REDSTONE_BLOCK.defaultBlockState());
            this.level.setBlockAndUpdate(BlockPos.containing(this.farmingBarrier.getBounds().centerBottom()), Blocks.RAIL.defaultBlockState());
            debounce = false;
            this.scheduler.addTask(new Task(this.game.time + SharedConstants.TICKS_PER_SECOND, (() -> {
                if (!this.isEntranceOpen) {
                    for (BlockPos bound : this.farmingBarrier.getBounds()) {
                        this.level.setBlock(bound, Blocks.BARRIER.defaultBlockState(), 2);
                    }
                    this.level.setBlockAndUpdate(this.farmingButtonRedstoneBlock, Blocks.AIR.defaultBlockState());
                }
                debounce = true;
            })));
        }

        for (PlantInfo plant : plants) {
            if ((gameSpace.getTime() - plant.tickPlaced()) % cropGrowTime == 0) {
                increasePlantAge(plant);
            }
        }

        this.trader.setGlowingTag(this.isFarmerGlowing);
        this.minecart.setGlowingTag(this.isMinecartGlowing);
    }

    private EventResult onEntityUse(ServerPlayer player, Entity entity, InteractionHand hand, EntityHitResult entityHitResult) {
        if (entity instanceof Chicken) {
            ItemStack item = player.getItemInHand(hand);
            if (seeds.contains(item.getItem())) {
                item.shrink(1);
                if (!player.getInventory().add(new ItemStack(Items.EGG))) {
                    level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), new ItemStack(Items.EGG)));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1, 1);
                return EventResult.DENY;
            }
        } else if (entity instanceof Mannequin mannequin) {
            if (mannequin.equals(trader)) {
                farmer.openUiForPlayer(player);
            }
        }
        return EventResult.PASS;
    }

    private void increasePlantAge(PlantInfo info) {
        BlockState state = this.level.getBlockState(info.pos());
        if (isCrop(state.getBlock())) {
            CropBlock block = (CropBlock) state.getBlock();
            block.growCrops(this.level, info.pos(), this.level.getBlockState(info.pos()));
        } else if (isPlant(state.getBlock())) {
            Block block = state.getBlock();
            if (block instanceof BonemealableBlock bonemealableBlock) {
                // cocoa or stem
                if (block.equals(Blocks.COCOA)) {
                    if (state.getValue(BlockStateProperties.AGE_2) < 2) {
                        bonemealableBlock.performBonemeal(this.level, this.level.getRandom(), info.pos(), state);
                    }
                } else if (block instanceof StemBlock stemBlock) {
                    if (state.getValue(BlockStateProperties.AGE_7) < 7) {
                        bonemealableBlock.performBonemeal(this.level, this.level.getRandom(), info.pos(), state);
                    } else {
                        if (!stemBlock.isValidBonemealTarget(level, info.pos(), state)){
                            growStem(stemBlock, info.pos());
                        }
                    }
                }
            } else if (block instanceof SugarCaneBlock) {
                BlockPos pos = info.pos();
                if (this.level.isEmptyBlock(pos.above())) {
                    int height = 1;
                    while (this.level.getBlockState(pos.below(height)).is(Blocks.SUGAR_CANE)) {
                        height++;
                    }
                    if (height < 3) {
                        IntegerProperty AGE = BlockStateProperties.AGE_15;
                        int age = state.getValue(AGE);
                        if (age >= 15) {
                            this.level.setBlockAndUpdate(pos.above(), Blocks.SUGAR_CANE.defaultBlockState());
                            this.level.setBlock(pos, state.setValue(AGE, 0), 260);
                        } else {
                            this.level.setBlock(pos, state.setValue(AGE, age + 5), 260);
                        }
                    }
                }
            }
        }
    }

    private void onBlockPlace(ServerPlayer player, ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (isPlant(blockState.getBlock())) {
            plants.add(new PlantInfo(blockPos, gameSpace.getTime()));
        }
    }

    private EventResult onBlockBreak(ServerPlayer serverPlayer, ServerLevel level, BlockPos blockPos) {
        if (isPlant(level.getBlockState(blockPos).getBlock())) {
            PlantInfo plantToRemove = getPlantByPos(blockPos);
            plants.remove(plantToRemove);
        }
        return EventResult.PASS;
    }

    private DroppedItemsResult droppedItemModifier(@Nullable Entity entity, ServerLevel level, BlockPos blockPos, BlockState blockState, List<ItemStack> itemStacks) {
        ArrayList<ItemStack> modifiedItems = new ArrayList<>(itemStacks);
        ArrayList<Item> itemsModified = new ArrayList<>();
        if (blockState.getBlock() instanceof CropBlock crop && !crop.isMaxAge(blockState)) {
            return DroppedItemsResult.pass(itemStacks);
        }
        modifiedItems.forEach(itemStack -> {
            Item item = itemStack.getItem();
            if (itemsModified.contains(item)) {
                itemStack.shrink(itemStack.getCount());
            } else {
                if (item.equals(Items.WHEAT_SEEDS)) {
                    itemStack.setCount(1);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.CARROT)) {
                    itemStack.setCount(2);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.POTATO)) {
                    itemStack.setCount(2);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.BEETROOT_SEEDS)) {
                    itemStack.setCount(1);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.BEETROOT)) {
                    itemStack.setCount(2);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.MELON_SEEDS)) {
                    itemStack.setCount(1);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.MELON)) {
                    itemStack.setCount(2);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.PUMPKIN_SEEDS)) {
                    itemStack.setCount(1);
                    itemsModified.add(itemStack.getItem());
                } else if (item.equals(Items.PUMPKIN)) {
                    itemStack.setCount(2);
                    itemsModified.add(itemStack.getItem());
                }
            }
        });
        return DroppedItemsResult.pass(modifiedItems);
    }
    private void growStem(StemBlock stemBlock, BlockPos pos) {
        StemBlockAccessor accessor = (StemBlockAccessor) stemBlock;
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
        BlockPos relative = pos.relative(direction);
        BlockState stateBelow = level.getBlockState(relative.below());
        if (level.getBlockState(relative).isAir() && stateBelow.is(accessor.cooking_frenzy$getFruitSupportBlocks())) {
            Registry<Block> blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
            Optional<Block> fruit = blocks.getOptional(accessor.cooking_frenzy$getFruit());
            Optional<Block> stem = blocks.getOptional(accessor.cooking_frenzy$getAttachedStem());
            if (fruit.isPresent() && stem.isPresent()) {
                level.setBlockAndUpdate(relative, (fruit.get()).defaultBlockState());
                level.setBlockAndUpdate(pos, (stem.get()).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
            }
        }
    }
    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (pos.equals(singlePlayerButtonPos)) {
            return InteractionResult.PASS;
        }
        if (stack.getItem().equals(Items.BONE_MEAL)) {
            if (state.getBlock().equals(Blocks.GRASS_BLOCK) || state.getBlock().equals(Blocks.SHORT_GRASS)) {
                return InteractionResult.FAIL;
            } else if (state.getBlock() instanceof StemBlock stemBlock) {
                if (!stemBlock.isValidBonemealTarget(level, pos, state)){
                    growStem(stemBlock, pos);
                    stack.shrink(1);
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    void onDisable(MalfunctionType reason) {
    }

    @Override
    void onEnable(MalfunctionType reason) {
    }

    record PlantInfo(BlockPos pos, long tickPlaced) { }
}
