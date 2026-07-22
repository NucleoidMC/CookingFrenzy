package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.extra.Customer;
import me.ellieis.cooking_frenzy.events.CustomerOrderTakenEvent;
import me.ellieis.cooking_frenzy.events.CustomerServedEvent;
import me.ellieis.cooking_frenzy.events.CustomerSpawnEvent;
import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.map.Map;
import me.ellieis.cooking_frenzy.map.MapWithCustomer;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyPhase;
import me.ellieis.cooking_frenzy.scheduler.CountdownTask;
import me.ellieis.cooking_frenzy.util.RayUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CustomerBehaviour<T extends Map> extends BaseBehaviour {
    final MapWithCustomer map;
    final CookingFrenzyPhase<T> game;
    TemplateRegion customerLights;
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Seat> seats = new ArrayList<>();
    ArrayList<Customer> customers = new ArrayList<>();
    ArrayList<Customer> customersToDespawn = new ArrayList<>();
    public int timeSinceLastCustomer;
    public int timeForCustomerSpawn;
    public int customerTimeout;
    public int orderPenalty;
    public boolean firstOrderSet = false;
    public BaseOrder firstOrder = null;
    boolean spawnCustomersAutomatically;
    public CustomerBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyPhase<T> game, int timeForCustomerSpawn, int customerTimeout, boolean spawnCustomersAutomatically) {
        super(gameSpace, activity, game.debugMode);
        this.map = (MapWithCustomer) game.map;
        this.game = game;
        this.customerLights = map.getCustomerLights();
        this.spawnCustomersAutomatically = spawnCustomersAutomatically;
        this.orderPenalty = Math.clamp(Math.round(game.gameState.money() / 0.25f), 15, 80);
        GameModifiers modifiers = this.game.gameState.currentModifiers();
        this.timeForCustomerSpawn = Math.round(timeForCustomerSpawn / modifiers.getModifier(GameModifiers.customerSpawnRateMultiplier));
        this.customerTimeout = Math.round(customerTimeout / modifiers.getModifier(GameModifiers.customerWaitingAngerRateMultiplier));
        for (TemplateRegion seatRegion : this.map.getSeats()) {
            Vec3 seatPos = seatRegion.getBounds().center();
            ArmorStand mount = new ArmorStand(this.game.level, seatPos.x(), seatPos.y() - 2, seatPos.z());
            int yaw = seatRegion.getData().getInt("yaw").orElseThrow();
            mount.setYRot(yaw);
            mount.setInvisible(true);
            mount.setInvulnerable(true);
            mount.addTag("NoGravity");
            this.game.level.addFreshEntity(mount);
            this.seats.add(new Seat(seatRegion.getData().getInt("id").orElseThrow(), mount, seatPos, yaw, false));
        }

        for (TemplateRegion nodeRegion : this.map.getCustomerNodes()) {
            ArrayList<Integer> seatIds = new ArrayList<>();
            for (Tag seats : nodeRegion.getData().getList("seats").orElseThrow()) {
                seatIds.add(seats.asInt().orElseThrow());
            }

            this.nodes.add(new Node(nodeRegion.getData().getInt("step").orElseThrow(), seatIds, nodeRegion.getBounds().centerBottom()));
        }
        if (this.spawnCustomersAutomatically) {
            this.spawnCustomer(true);
        }
    }
    public CustomerBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyPhase<T> game, boolean spawnCustomersAutomatically) {
        this(gameSpace, activity, game, SharedConstants.TICKS_PER_MINUTE - ((Math.min(30, 5 * game.gameState.dayCount()) * SharedConstants.TICKS_PER_SECOND)), (90 - (2 * game.gameState.dayCount())) * SharedConstants.TICKS_PER_SECOND, spawnCustomersAutomatically);
    }
    protected void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(EntityUseEvent.EVENT, this::onUse);
    }

    private EventResult onUse(ServerPlayer player, Entity entity, InteractionHand hand, EntityHitResult entityHitResult) {
        if (entity instanceof Mannequin mannequin) {
            for (Customer customer : customers) {
                if (customer.entity.equals(mannequin)) {
                    customer.onInteract(player, hand);
                    return EventResult.DENY;
                }
            }
        }
        return EventResult.PASS;
    }

    private void onTick() {
        if (firstOrderSet && firstOrder != null) {
            if (firstOrder.food().equals(Items.BREAD)) {
                this.game.gameState = this.game.gameState.incrementMoney(40);
                this.firstOrder = null;
            }
        }
        customersToDespawn.clear();
        this.timeSinceLastCustomer++;
        for (Customer customer : customers) {
            if (customer.despawnFlag) {
                customersToDespawn.add(customer);
                continue;
            }
            customer.tick();
        }

        ArrayList<Customer> beingLookedAt = new ArrayList<>();
        for (ServerPlayer player : this.gameSpace.getPlayers().participants()) {
            EntityHitResult result = RayUtils.raytrace(player, 5, 0.5, (entity -> entity instanceof Mannequin));
            if (result != null) {
                for (Customer customer : customers) {
                    if (result.getEntity().equals(customer.entity)) {
                        beingLookedAt.add(customer);
                    }
                }
            }
        }
        for (Customer customer : this.customers) {
            customer.beingLookedAt = beingLookedAt.contains(customer);
        }


        for (Customer customer : customersToDespawn) {
            customer.entity.remove(Entity.RemovalReason.KILLED);
            setSeat(customer.seat.setHasCustomer(false));
            customers.remove(customer);
            customer.cleanUp();
        }

        for (Seat seat: seats) {
            Vec3 seatPos = seat.position();
            seat.entity.teleportTo(this.game.level, seatPos.x(), seatPos.y() - 2, seatPos.z(), Set.of(), seat.yaw(), 0, false);
        }

        BlockState bulbState;
        if (!this.getPendingOrders().isEmpty()) {
            bulbState = Blocks.COPPER_BULB.waxed().weathered().defaultBlockState().setValue(BlockStateProperties.LIT, true);
        } else {
            bulbState = Blocks.COPPER_BULB.waxed().weathered().defaultBlockState();
        }
        for (BlockPos bound : customerLights.getBounds()) {
            this.game.level.setBlock(bound, bulbState, 2);
        }

        if (this.timeSinceLastCustomer  >= this.timeForCustomerSpawn) {
            if (!this.spawnCustomersAutomatically) {
                return;
            }
            this.spawnCustomer(true);
            this.timeSinceLastCustomer = 0;
        }
    }

    private Seat getSeatById(int id) {
        return getSeatById(id, seats);
    }

    private Seat getSeatById(int id, ArrayList<Seat> list) {
        for (Seat seat : list) {
            if (seat.id == id) {
                return seat;
            }
        }
        return null;
    }

    private void setSeat(Seat seat) {
        seats.set(seats.indexOf(this.getSeatById(seat.id())), seat);
    }

    private Seat getAvailableSeat() {
        for (Seat seat: seats) {
            if (!seat.hasCustomer()) {
                return seat;
            }
        }
        return null;
    }

    public void setScore(int score) {
        game.gameState = game.gameState.decrementCustomerCount().incrementMoney(score);
        if (score >= 50) {
            // bring customers in faster if you're good at it
            this.timeSinceLastCustomer += SharedConstants.TICKS_PER_SECOND * 30;
        } else if (score >= 45) {
            this.timeSinceLastCustomer += SharedConstants.TICKS_PER_SECOND * 15;
        }
    }

    public void spawnCustomer(boolean spawnMultiple) {
        List<TemplateRegion> customerSpawns = this.map.getCustomerSpawns();
        Seat seat = this.getAvailableSeat();
        if (seat == null) {
            return;
        }

        if (spawnMultiple) {
            int day = this.game.gameState.dayCount();
            int doubleCustomerChance = -1;
            int tripleCustomerChance = -1;
            if (day == 2) {
                doubleCustomerChance = 30;
            } else if (day == 3) {
                doubleCustomerChance = 40;
                tripleCustomerChance = 10;
            } else if (day == 4) {
                tripleCustomerChance = 20;
                doubleCustomerChance = 50;
            } else if (day >= 5) {
                tripleCustomerChance = 30;
                doubleCustomerChance = 65;
            }
            int execTime = 0;
            if (ThreadLocalRandom.current().nextInt(0, 100) <= tripleCustomerChance) {
                execTime = 40;
            } else if (ThreadLocalRandom.current().nextInt(0, 100) <= doubleCustomerChance) {
                execTime = 20;
            }
            if (execTime != 0) {
                this.game.scheduler.addCountdown(new CountdownTask(execTime, (time -> {
                    if (time == 0) {
                        return;
                    }
                    spawnCustomer(false);
                })));
            }
        }

        Vec3 spawn = customerSpawns.get(ThreadLocalRandom.current().nextInt(customerSpawns.size())).getBounds().centerBottom();
        ArrayList<Node> path = calculatePathForSeat(seat);
        // there are two node 0s, because there's two spawn positions. need to remove the other spawn pos
        Node nodeToDelete = null;
        for (Node node : path) {
            if (node.step() == 0) {
                if (node.position().distanceTo(spawn) >= 2) {
                    nodeToDelete = node;
                    break;
                }
            }
        }
        path.remove(nodeToDelete);
        List<ServerPlayer> players = this.game.gameSpace.getServer().getPlayerList().getPlayers();
        ResolvableProfile profile;
        if (ThreadLocalRandom.current().nextFloat() >= 0.999) {
            // haykam jumpscare
            profile = ResolvableProfile.createUnresolved(UUID.fromString("c705fc7d-0962-4d1e-905b-b83d81d9b4ec"));
        } else {
            profile = players.get(ThreadLocalRandom.current().nextInt(0, players.size())).getProfile();
        }
        GameModifiers modifiers = this.game.gameState.currentModifiers();
        Customer customer = new Customer(path, this.game.level, spawn, this, modifiers.getModifier(GameModifiers.customerWaitingAngerRateMultiplier), modifiers.getModifier(GameModifiers.customerOrderAngerRateMultiplier), seat, this.game.gameState.tier(), profile);
        if (Stimuli.select().forEntity(customer.entity).get(CustomerSpawnEvent.EVENT).onCustomerSpawn(customer) == EventResult.DENY) {
            return;
        }
        customer.advanceNode(false);
        customers.add(customer);
        setSeat(seat.setHasCustomer(true));
        this.game.gameState = this.game.gameState.incrementCustomerCount();
    }

    public ArrayList<AvailableOrder> getAvailableOrders() {
        ArrayList<AvailableOrder> orders = new ArrayList<>();
        for (Customer customer : this.customers) {
            if (customer.currentOrder == null || !customer.interactible) continue;
            int maxTime = customer.currentOrder.timeLimit();
            int currentTime = customer.timeout;
            orders.add(new AvailableOrder(maxTime, currentTime, customer.currentOrder.food().getDefaultInstance().getItemName()));
        }
        return orders;
    }

    public ArrayList<AvailableOrder> getPendingOrders() {
        ArrayList<AvailableOrder> orders = new ArrayList<>();
        for (Customer customer: this.customers) {
            if (customer.currentOrder != null || !customer.interactible) continue;
            orders.add(new AvailableOrder(customerTimeout, customer.timeout, Component.empty()));
        }
        return orders;
    }

    private ArrayList<Node> calculatePathForSeat(Seat seat) {
        ArrayList<Node> steps = new ArrayList<>();
        for (Node node : this.nodes) {
            if (node.seats.contains(seat.id())) {
                steps.add(node);
            }
        }
        return steps;
    }

    public void onCustomerServed(Customer customer) {
        Stimuli.select().forEntity(customer.entity).get(CustomerServedEvent.EVENT).onCustomerServed(customer);
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_SERVED)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 1, 1, player.level().getSeed()));
        }
    }

    public void onCustomerLeave() {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CustomSounds.CUSTOMER_LEAVE)), SoundSource.AMBIENT, player.getX(), player.getY(), player.getZ(), 1, 1, player.level().getSeed()));
        }
    }

    public record Node(int step, ArrayList<Integer> seats, Vec3 position) { }

    public record Seat(int id, ArmorStand entity, Vec3 position, int yaw, boolean hasCustomer) {
        public Seat setHasCustomer(boolean val) {
            return new Seat(this.id, this.entity, this.position, this.yaw, val);
        }
    }

    public record AvailableOrder(int timeLimit, int timeLeft, Component itemName) {

    }
}
