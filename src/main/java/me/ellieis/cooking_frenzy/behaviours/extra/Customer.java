package me.ellieis.cooking_frenzy.behaviours.extra;

import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.CustomerBehaviour;
import me.ellieis.cooking_frenzy.events.CustomerOrderTakenEvent;
import me.ellieis.cooking_frenzy.events.CustomerSitEvent;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.gamestate.orders.Orders;
import me.ellieis.cooking_frenzy.mixins.MannequinAccessor;
import me.ellieis.cooking_frenzy.ui.ProgressBarComponent;
import me.ellieis.cooking_frenzy.ui.spatial.attachment.PopUpAttachment;
import me.ellieis.cooking_frenzy.ui.spatial.holder.DissapearingHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

import java.util.ArrayList;
import java.util.Set;

import static me.ellieis.cooking_frenzy.ui.Common.mapRange;

public class Customer {
    ArrayList<CustomerBehaviour.Node> nodes;
    int currentNodeId;
    Vec3 pointToNavigate;
    Vec3 oldPos;
    public CustomerBehaviour.Seat seat;
    ArmorStand seatEntity;
    public Mannequin entity;
    ServerLevel level;
    double distanceToPoint;
    long timeToWalk;
    long walkCounter;
    double alpha = 0;
    float currentYaw;
    boolean hasReachedSeat = false;
    public boolean despawnFlag = false;
    int recipeTier;
    public BaseOrder currentOrder = null;
    Display.ItemDisplay orderDisplay;
    public boolean interactible = false;
    public boolean beingLookedAt = false;
    public int timeout;
    CustomerBehaviour behaviour;
    Display.TextDisplay timeoutBar;
    float waitingAngerRateMultiplier;
    float orderAngerRateMultiplier;
    public Customer(ArrayList<CustomerBehaviour.Node> nodes, ServerLevel level, Vec3 spawnPos, CustomerBehaviour behaviour, float waitingAngerRateMultiplier, float orderAngerRateMultiplier, CustomerBehaviour.Seat seat, int recipeTier, ResolvableProfile skin) {
        this.nodes = nodes;
        this.level = level;
        this.currentNodeId = 0;
        this.seat = seat;
        this.recipeTier = recipeTier;
        this.entity = new Mannequin(EntityTypes.MANNEQUIN, this.level);
        ((MannequinAccessor) this.entity).cooking_frenzy$setMannequinProfile(skin);
        this.entity.setInvulnerable(true);
        this.entity.teleportTo(spawnPos.x(), spawnPos.y(), spawnPos.z());
        this.behaviour = behaviour;
        this.waitingAngerRateMultiplier = waitingAngerRateMultiplier;
        this.orderAngerRateMultiplier = orderAngerRateMultiplier;
        this.timeout = Math.round(this.behaviour.customerTimeout / this.waitingAngerRateMultiplier);
        if (behaviour.debugMode) {
            System.out.println("Customer waiting Timeout: " + timeout);
        }
        this.seatEntity = seat.entity();
        this.oldPos = this.entity.position();
        this.timeoutBar = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, this.level);
        this.orderDisplay = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, this.level);
        this.entity.playSound(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_ARRIVED));
        this.level.addFreshEntity(this.entity);
    }

    private void sitDown() {
        this.hasReachedSeat = true;
        this.interactible = true;
        this.currentYaw = this.seat.yaw();
        Vec3 pos = seat.position();
        this.entity.teleportTo(this.level, pos.x(), pos.y(), pos.z(), Set.of(), currentYaw, 0, false);
        this.entity.startRiding(seatEntity, true, true);
        this.entity.addTag("NoGravity");
        this.entity.playSound(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_RING_BELL));
        this.timeoutBar.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        this.timeoutBar.setBackgroundColor(0);
        this.level.addFreshEntity(this.timeoutBar);
        Stimuli.select().forEntity(this.entity).get(CustomerSitEvent.EVENT).onCustomerSit(this);
    }

    private void sitUp() {
        this.entity.stopRiding();
        this.entity.removeTag("NoGravity");
        Vec3 pos = oldPos;
        this.entity.teleportTo(this.level, pos.x(), pos.y(), pos.z(), Set.of(), currentYaw, 0, false);
        advanceNode(true);
    }
    /**
     * Advances through the node tree
     * @param reverse Whether to walk the node tree in reverse (example, going back to spawn after sitting)
     * @return if it was able to trace a path
     */
    public boolean advanceNode(boolean reverse) {
        int oldNodeId = currentNodeId;
        this.oldPos = this.entity.position();
        if (reverse) {
            currentNodeId--;
        } else {
            currentNodeId++;
        }

        if (reverse) {
            if (currentNodeId < 0) {
                this.despawnFlag = true;
                return false;
            }
        } else if (currentNodeId >= nodes.size()) {
            this.sitDown();
            return false;
        }

        CustomerBehaviour.Node currentNode = null;
        CustomerBehaviour.Node oldNode = null;
        for (CustomerBehaviour.Node node : nodes) {
            if (node.step() == currentNodeId) {
                currentNode = node;
            } else if (node.step() == oldNodeId) {
                oldNode = node;
            }
        }

        if (currentNode == null) {
            this.pointToNavigate = null;
            return false;
        }
        if (oldNode == null) {
            oldNode = new CustomerBehaviour.Node(this.currentNodeId, new ArrayList<>(), currentNode.position());
        }
        this.pointToNavigate = currentNode.position();
        this.distanceToPoint = oldNode.position().distanceTo(this.pointToNavigate);
        // 4.317 is the base walk speed in blocks
        this.timeToWalk = (long) ((this.distanceToPoint / (4.317 / 2)) * 20);
        this.walkCounter = 0;
        this.alpha = 1;
        double dx = pointToNavigate.x - oldPos.x;
        double dz = pointToNavigate.z - oldPos.z;
        this.currentYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return true;
    }

    private void pathfind() {
        walkCounter++;
        this.alpha = (double) walkCounter  / timeToWalk;
        if (this.alpha > 1) {
            if (advanceNode(hasReachedSeat)) {
                this.alpha = (double) walkCounter / timeToWalk;
            } else {
                this.pointToNavigate = null;
                return;
            }
        }
        Vec3 pos = oldPos.lerp(this.pointToNavigate, this.alpha);
        this.entity.teleportTo(this.level, pos.x(), pos.y(), pos.z(), Set.of(), currentYaw, 0, false);
    }

    private void setScore(int score) {
        boolean isPositive = score >= 0;
        DissapearingHolder holder = new DissapearingHolder(150);
        String text = (isPositive ? "+" : "") +
                score;
        TextDisplayElement display = new TextDisplayElement(Component.literal(text).withStyle(isPositive ? ChatFormatting.GREEN : ChatFormatting.RED));
        display.setBillboardMode(Display.BillboardConstraints.CENTER);
        display.setBackground(0);
        holder.addElement(display);
        PopUpAttachment.of(holder, this.level, this.entity.position().add(0, 2, 0), 100, 0.5f);
        behaviour.setScore(score);
    }
    public void cleanUp() {
        this.currentOrder = null;
        this.seat = null;
        this.seatEntity = null;
    }

    public void tick() {
        if (this.pointToNavigate != null) {
            this.pathfind();
        }
        if (this.hasReachedSeat && interactible) {
            if (this.timeout <= 0) {
                this.entity.setXRot(0);
                this.setScore(-20);
                this.sitUp();
                this.timeoutBar.remove(Entity.RemovalReason.KILLED);
                this.orderDisplay.remove(Entity.RemovalReason.KILLED);
                this.interactible = false;
                this.behaviour.onCustomerLeave();
            } else {
                this.timeout--;
                float limit = (this.currentOrder != null ? this.currentOrder.timeLimit() / this.orderAngerRateMultiplier: this.behaviour.customerTimeout / this.waitingAngerRateMultiplier);
                if (this.beingLookedAt) {
                    if (this.currentOrder == null) {
                        this.timeoutBar.teleportTo(this.level, this.entity.getX(), this.entity.getY() + 2, this.entity.getZ(), Set.of(), 0, 0, false);
                    } else {
                        this.timeoutBar.teleportTo(this.level, this.entity.getX(), this.entity.getY() + 2.35, this.entity.getZ(), Set.of(), 0, 0, false);
                    }
                } else {
                    this.timeoutBar.teleportTo(this.level, this.entity.getX(), this.entity.getY() - 3, this.entity.getZ(), Set.of(), 0, 0, false);
                }
                this.entity.setXRot(mapRange(Math.min(this.timeout, limit), 0, limit, 75, 0));
                this.timeoutBar.setText(ProgressBarComponent.create(6, this.timeout, 0, limit, true));
            }
        }
        this.entity.setYHeadRot(this.currentYaw);
    }

    public void onInteract(ServerPlayer player, InteractionHand hand) {
        if (player.gameMode() != GameType.SURVIVAL) {
            return;
        }
        ItemStack item = player.getItemInHand(hand);
        if (hasReachedSeat && interactible) {
            if (this.currentOrder == null) {
                this.currentOrder = Orders.random(this.recipeTier, level.getRandom());
                var result = Stimuli.select().forEntity(this.entity).get(CustomerOrderTakenEvent.EVENT).onCustomerOrderTaken(this, currentOrder);
                if (result.result() != EventResult.PASS) {
                    this.currentOrder = result.order();
                }
                if (!this.behaviour.firstOrderSet) {
                    this.behaviour.firstOrderSet = true;
                    this.behaviour.firstOrder = this.currentOrder;
                }

                this.level.playSound(null, this.entity.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS);
                this.orderDisplay.teleportTo(this.level, this.entity.getX(), this.entity.getY() + 2, this.entity.getZ(), Set.of(), 0, 0, false);
                this.orderDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
                this.orderDisplay.setItemStack(this.currentOrder.food().getDefaultInstance());
                this.orderDisplay.setItemTransform(ItemDisplayContext.GROUND);
                this.level.addFreshEntity(this.orderDisplay);
                this.timeout = Math.round(this.currentOrder.timeLimit() / this.orderAngerRateMultiplier);
                if (this.behaviour.debugMode) {
                    System.out.println("Customer order timeout: " + this.timeout);
                }
            } else if (!item.isEmpty()) {
                if (item.getItem() == this.currentOrder.food()) {
                    this.interactible = false;
                    this.level.playSound(null, this.entity.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS);
                    item.shrink(1);
                    int timeLimit = this.currentOrder.timeLimit();
                    this.orderDisplay.remove(Entity.RemovalReason.KILLED);
                    this.setScore((Math.round((60 * Math.max(0.5f, ((float) this.timeout / timeLimit))))));
                    this.timeoutBar.remove(Entity.RemovalReason.KILLED);
                    sitUp();
                    this.behaviour.onCustomerServed(this);
                }
            }
        }
    }
}