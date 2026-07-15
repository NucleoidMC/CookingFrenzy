package me.ellieis.cooking_frenzy.behaviours;

import com.mojang.math.Transformation;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.events.MeatDispensedEvent;
import me.ellieis.cooking_frenzy.events.TargetBlockHit;
import me.ellieis.cooking_frenzy.map.MapWithFreezer;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import me.ellieis.cooking_frenzy.ui.ProgressBarComponent;
import me.ellieis.cooking_frenzy.ui.spatial.holder.DissapearingHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

import java.util.List;

public class FreezerBehaviour extends BaseBehaviour {
    final BarrelBlockEntity snowballsContainer;
    final Display.TextDisplay snowballDisplay;
    final ServerLevel level;
    final MapWithFreezer map;
    final Scheduler scheduler;
    boolean freezerDoorOpen;
    boolean freezerPlatePressed;
    List<TemplateRegion> freezerPlates;
    List<TemplateRegion> meatProviders;
    TemplateRegion freezerArea;
    BlockPos freezerDoorPos;
    int snowballTimer = 0;
    int timeForSnowball;
    public FreezerBehaviour(GameSpace gameSpace, GameActivity activity, ServerLevel level, MapWithFreezer map, Scheduler scheduler, float snowballMultiplier, boolean debugMode) {
        super(gameSpace, activity, debugMode);
        this.level = level;
        this.map = map;
        this.scheduler = scheduler;
        this.timeForSnowball = Math.round((45 * SharedConstants.TICKS_PER_SECOND) / snowballMultiplier);
        if (debugMode) {
            System.out.println("Snowball timer: " + timeForSnowball);
        }

        if (this.level.getBlockEntity(BlockPos.containing(this.map.getSnowballContainer().getBounds().center())) instanceof BarrelBlockEntity be) {
            this.snowballsContainer = be;
            this.snowballsContainer.setItem(13, new ItemStack(Items.SNOWBALL, 3));
        } else {
            throw new GameOpenException(Component.literal("Could not find snowball container"));
        }
        Display.TextDisplay title = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, this.level);

        Vec3 pos = Vec3.atCenterOf(snowballsContainer.getBlockPos().above()).add(0, 0, 0.52);
        title.teleportTo(pos.x(), pos.y(), pos.z());
        title.setBackgroundColor(0);
        title.setText(Component.translatable("cooking_frenzy.freezer.snowball_gen"));
        title.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.4f, 0.4f, 0.1f), new Quaternionf()));
        this.snowballDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, this.level);
        pos = Vec3.atCenterOf(snowballsContainer.getBlockPos().above()).add(0, -0.25, 0.52);
        snowballDisplay.teleportTo(pos.x(), pos.y(), pos.z());
        snowballDisplay.setBillboardConstraints(Display.BillboardConstraints.FIXED);
        snowballDisplay.setBackgroundColor(0);
        snowballDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.2f, 1, 0.1f), new Quaternionf()));
        this.level.addFreshEntity(title);
        this.level.addFreshEntity(snowballDisplay);
        this.freezerPlates = this.map.getFreezerPlates();
        this.freezerDoorPos = BlockPos.containing(map.getFreezerDoor().getBounds().center());
        this.meatProviders = map.getMeatProviders();
        this.freezerArea = map.getFreezerArea();
    }

    void setupEvents() {
        this.activity.listen(TargetBlockHit.EVENT, this::onTargetHit);
        this.activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void onTick() {
        this.checkFreezerDoorPlates();
        this.checkSnowballs();
        for (ServerPlayer player : this.gameSpace.getPlayers().participants()) {
            if (this.freezerArea.getBounds().contains(BlockPos.containing(player.getPosition(0)))) {
                // needs to be 3+ because the player loses 2 per tick while outside powder snow
                player.setTicksFrozen(Math.min(player.getTicksFrozen() + 6, 150));
            }
        }
    }

    private void checkSnowballs() {
        snowballTimer++;
        if ((snowballTimer % (timeForSnowball)) == 0) {
            int snowballs = snowballsContainer.countItem(Items.SNOWBALL);
            if (snowballs < 5) {
                snowballsContainer.clearContent();
                snowballsContainer.setItem(13, new ItemStack(Items.SNOWBALL, snowballs + 1));
                snowballTimer = 0;
            } else {
                snowballTimer--;
            }
        }
        snowballDisplay.setText(ProgressBarComponent.create(20, snowballTimer, 0, 90 * SharedConstants.TICKS_PER_SECOND, true, ChatFormatting.BLUE));
    }

    private void checkFreezerDoorPlates() {
        if (!this.freezerDoorOpen) {
            int poweredPlates = 0;
            for (TemplateRegion plate : freezerPlates) {
                if (this.level.getBlockState(BlockPos.containing(plate.getBounds().center())).getValue(PressurePlateBlock.POWERED)) {
                    poweredPlates++;
                }
            }
            if (poweredPlates >= freezerPlates.size()) {
                BlockState state = level.getBlockState(freezerDoorPos);
                state = state.cycle(DoorBlock.OPEN);
                level.setBlock(freezerDoorPos, state, 10);
                level.playSound(null, freezerDoorPos, BlockSetType.IRON.doorClose(), SoundSource.BLOCKS);
                this.freezerDoorOpen = true;
                this.freezerPlatePressed = true;
                // closing task only gets scheduled once the plate is unpowered
            }
            for (TemplateRegion plate: freezerPlates) {
                if (this.level.getBlockEntity(BlockPos.containing(plate.getBounds().center()).above()) instanceof SignBlockEntity sign) {
                    Component[] text = {
                            Component.translatable("cooking_frenzy.freezer.sign.top"),
                            Component.translatable("cooking_frenzy.freezer.sign.bottom", poweredPlates, freezerPlates.size()),
                            Component.empty(),
                            Component.empty()
                    };
                    Component[] filteredText = {
                            Component.empty(),
                            Component.empty(),
                            Component.empty(),
                            Component.empty()
                    };
                    sign.setText(new SignText(text, filteredText, DyeColor.WHITE, true), true);
                }
            }
        } else if (this.freezerPlatePressed) {
            List<TemplateRegion> freezerPlates = this.map.getFreezerPlates();
            int poweredPlates = 0;
            for (TemplateRegion plate : freezerPlates) {
                if (this.level.getBlockState(BlockPos.containing(plate.getBounds().center())).getValue(PressurePlateBlock.POWERED)) {
                    poweredPlates++;
                }
            }
            if (poweredPlates < freezerPlates.size()) {
                // closing task only gets scheduled once the plate is unpowered
                this.freezerPlatePressed = false;
                BlockState newState = level.getBlockState(freezerDoorPos);
                newState = newState.cycle(DoorBlock.OPEN);
                level.setBlock(freezerDoorPos, newState, 10);
                level.playSound(null, freezerDoorPos, BlockSetType.IRON.doorClose(), SoundSource.BLOCKS);
                this.freezerDoorOpen = false;
            }
        }
    }

    private void spawnHitDisplay(boolean hitTarget, Vec3 position, Direction direction) {
        DissapearingHolder holder = new DissapearingHolder(100);
        MutableComponent component;
        if (hitTarget) {
            component = Component.literal("✓").withStyle(ChatFormatting.GREEN);
        } else {
            component = Component.literal("X").withStyle(ChatFormatting.RED);
        }
        TextDisplayElement display = new TextDisplayElement(component);
        display.setRotation(0, direction.toYRot());
        display.setBackground(0);
        if (!hitTarget) {
            display.setScale(new Vector3f(0.6f, 0.6f, 0.6f));
        }
        holder.addElement(display);
        ChunkAttachment.ofTicking(holder, level, position.relative(direction, 0.1));
    }

    private EventResult onTargetHit(BlockHitResult hitResult, Entity entity, int powerLevel) {
        if (powerLevel >= 6) {
            spawnHitDisplay(true, entity.position(), hitResult.getDirection());
            for (TemplateRegion region : meatProviders) {
                if (region.getBounds().contains(hitResult.getBlockPos())) {
                    region.getData().getString("type").ifPresent(meatType -> {
                        Item item = null;
                        switch (meatType) {
                            case "chicken" -> {
                                item = Items.CHICKEN;
                            }
                            case "pork" -> {
                                item = Items.PORKCHOP;
                            }
                            case "beef" -> {
                                item = Items.BEEF;
                            }
                        }
                        if (item != null) {
                            Vec3 pos = map.getFoodDropper().getBounds().center();
                            this.level.addFreshEntity(new ItemEntity(this.level, pos.x(), pos.y(), pos.z(), new ItemStack(item), 0, 0, 0));
                            this.level.playSound(null, BlockPos.containing(pos), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1, 1);
                            Stimuli.select().at(this.level, BlockPos.containing(pos)).get(MeatDispensedEvent.EVENT).onMeatDispensed(item);
                        }
                    });
                }
            }
        } else {
            spawnHitDisplay(false, entity.position(), hitResult.getDirection());
            Vec3 pos = map.getFoodDropper().getBounds().center();
            this.level.playSound(null, BlockPos.containing(pos), SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE), SoundSource.BLOCKS, 5, 1);
        }
        return EventResult.PASS;
    }
}
