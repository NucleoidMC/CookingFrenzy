package me.ellieis.cooking_frenzy.behaviours.extra;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FreezerMaintenanceNPC extends PathfinderNPC {
    public boolean hasReachedDoor = false;
    public boolean isFixing = false;
    public boolean isFixed = false;
    int fixingTimer = 0;
    Display.ItemDisplay itemDisplay;
    CookingFrenzyActive game;
    public FreezerMaintenanceNPC(ArrayList<Node> nodes, ServerLevel level, Vec3 spawnPos, CookingFrenzyActive game) {
        GameProfile skin = new GameProfile(
                UUID.fromString("75f47286-b9c3-4af5-a3d7-6d3941bc8e04"),
                "skin",
                new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTcwMjQ2MDA4MDY2MCwKICAicHJvZmlsZUlkIiA6ICJjMTRlMWM3OTM5NGM0MzNjOThmYzg4MjQzOTg5ZTc5MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJMb293b3AiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmRjZThiNzJkYzQ1NTJkZDIyNGQ1NTFkZThmYzU3NWE5MGY5ZDJmYTEzMzcxMGMyOGY5ZGRiM2QyYmM5NTA1NSIKICAgIH0KICB9Cn0=", "FUqagt37LRZtYyaCIsFkg1LA5tRbhz7Sbx+CqOZ+dKQpajXLj3Zn4KkoS0oM96SEeeoWCVdap2s7jh5mmko75848Ub6i+FPYBv8yYaOY7d1DigGK0GZgNnnWyPbkeWSk0ArfQ6g+ZWEKTDmjHGDDt5akg/qgraNAvtVIffn64iok/ihFCExo2jBLQvDZk1nudRC8WXgWPoqIgOA6UhakRV3WV2le6YDL7SBGYGBhnMY76BL5Fi4n0kRCK2vii5fZOpDimaQekZC+4hzwfWD1fr8Y86S7kOVMhVmRdRIh4Kj2JaVwxZcZkO4ZpmiBw+hKK2XKfojUaL+zL3XGeEbeEkrrvl3VSNDDI/AqOcgi+ueiPH2NfT0OYVPf8M2K4e9X2X7br7l7J2WIh26wEYv61yrhZaVpeV96sj2t3V2eDs88IMTRMc4QqM4bdKBPNYDcsP8yPyDBJzs46vJTdwjQgXPlpUyn36ik/jxmPHyP3fie0Vjtmv4waRxiGR8tJA8QFe5WFXHWOfENfrDHTf0zegT/RGftHmMMpfKOlrnt7nTEWI1F/7iDqRip/PQ53fAXXZaxrkHY7vibUAMERRLVXjSRev7Q00VHy+aJF+vfDN3N+llV6XDahWfeWJl3WY/Lo/hKDSBFMAKEk29o9uiJke3PcpFH430g5Cv/tFrB3rQ=")))
        );

        super(nodes.stream().map((node -> new Node(node.step(), node.position()))).collect(Collectors.toCollection(ArrayList::new)), level, spawnPos, ResolvableProfile.createResolved(skin));
        this.game = game;
        this.advanceNode(false);
    }
    @Override
    void onPathFinished() {
        if (!hasReachedDoor) {
            Vec3 pos = this.entity.position();
            this.entity.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), 180, 0, false);
            hasReachedDoor = true;
            setShouldWalkReversePath(true);
            this.itemDisplay = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            itemDisplay.setItemStack(new ItemStack(Items.SHEARS));
            itemDisplay.teleportTo(this.level, this.entity.getX(), this.entity.getY() + 2, this.entity.getZ(), Set.of(), 0, 0, false);
            itemDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            itemDisplay.setItemTransform(ItemDisplayContext.GROUND);
            level.addFreshEntity(itemDisplay);
            game.gameSpace.getPlayers().sendMessage(Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance.dialog", Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance.maintenance_guy").withStyle(ChatFormatting.YELLOW), Items.SHEARS.getDefaultInstance().getItemName().copy().withStyle(ChatFormatting.GREEN)));
            game.gameSpace.getPlayers().playSound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1, 1);
            this.entity.setGlowingTag(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (hasReachedDoor) {
            Vec3 pos = this.entity.position();
            this.entity.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), this.entity.getYRot(), 0, false);
        }
        if (isFixing) {
            if (fixingTimer <= 0) {
                this.isFixing = false;
                this.isFixed = true;
                this.setShouldWalkReversePath(true);
                advanceNode(true);
                game.gameSpace.getPlayers().sendMessage(Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance.dialog2", Component.translatable("cooking_frenzy.malfunctions.freezer_maintenance.maintenance_guy").withStyle(ChatFormatting.YELLOW)));
                game.gameSpace.getPlayers().playSound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1, 1);
            } else {
                fixingTimer--;
            }
        }
    }

    public void onInteract(ItemStack itemStack) {
        if (itemStack.getItem().equals(Items.SHEARS) && !isFixing) {
            isFixing = true;
            itemStack.shrink(1);
            itemDisplay.remove(Entity.RemovalReason.KILLED);
            fixingTimer = 20 * SharedConstants.TICKS_PER_SECOND;
            Vec3 pos = this.entity.position();
            this.level.playSound(null, this.entity.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS);
            this.entity.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), 0, 0, false);
            this.entity.setItemInHand(InteractionHand.MAIN_HAND, Items.SHEARS.getDefaultInstance());
            this.entity.setGlowingTag(false);
            this.game.scheduler.addTask(new Task(this.game.time + SharedConstants.TICKS_PER_SECOND * 3, () -> {
                this.level.playSound(null, this.entity.blockPosition(), SoundEvent.createVariableRangeEvent(CustomSounds.FIXING_DOOR), SoundSource.PLAYERS);
            }));
        }
    }
}
