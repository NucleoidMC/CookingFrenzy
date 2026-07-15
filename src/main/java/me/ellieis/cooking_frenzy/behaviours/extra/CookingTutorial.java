package me.ellieis.cooking_frenzy.behaviours.extra;

import me.ellieis.cooking_frenzy.behaviours.CustomerBehaviour;
import me.ellieis.cooking_frenzy.behaviours.TutorialBehaviour;
import me.ellieis.cooking_frenzy.events.*;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;

public class CookingTutorial extends BaseTutorial {
    CustomerBehaviour<Active> behaviour;
    boolean customerSat = false;
    boolean orderTaken = false;
    boolean targetHit = false;
    boolean chickenCooked = false;
    boolean orderServed = false;
    boolean stallingCamera = false;
    public CookingTutorial(TutorialBehaviour tutorial, GameActivity activity, GameSpace gameSpace, Active map, Scheduler scheduler, CookingFrenzyActive game, CustomerBehaviour<Active> behaviour) {
        super(tutorial, activity, gameSpace, map, scheduler, game);
        this.behaviour = behaviour;
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void onTick() {
        if (stallingCamera) {
            tutorial.timeUntilAngleEnds = 999;
        }
    }

    public void startTutorial() {
        stallingCamera = true;
        tutorial.setCameraAngle(map.tutorialCameraPositions.customer(), 999);
        behaviour.spawnCustomer(false);
        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived"));
        activity.listen(CustomerSitEvent.EVENT, (_customer) -> {
            if (customerSat) return;
            _customer.timeout = 10 * SharedConstants.TICKS_PER_MINUTE;
            customerSat = true;
            tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.take_order"));
            stallingCamera = false;
            tutorial.timeUntilAngleEnds = 5 * SharedConstants.TICKS_PER_SECOND;
            activity.listen(CustomerOrderTakenEvent.EVENT, (customer, order) -> {
                if (orderTaken) {
                    return OrderTakenResult.pass(order);
                }
                orderTaken = true;
                tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.order_taken", Items.COOKED_CHICKEN.getDefaultInstance().getItemName()));
                scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 4), () -> {
                    Vec3 oldPos = tutorial.player.position();
                    tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.freezer.outside"));
                    tutorial.setCameraAngle(map.tutorialCameraPositions.freezerOutside(), 5 * SharedConstants.TICKS_PER_SECOND, oldPos);
                    scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 5), () -> {
                        tutorial.setCameraAngle(map.tutorialCameraPositions.freezerInside(), 5 * SharedConstants.TICKS_PER_SECOND, oldPos);
                        scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 5), () -> {
                            tutorial.setCameraAngle(map.tutorialCameraPositions.freezerTarget(), SharedConstants.TICKS_PER_SECOND * 4, oldPos);
                            tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.freezer.target"));
                            activity.listen(ItemPickupEvent.EVENT, (player, itemEntity, meat) -> {
                                if (meat.getItem().equals(Items.CHICKEN) && !targetHit) {
                                    targetHit = true;
                                    tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.cook"));
                                    tutorial.setCameraAngle(map.tutorialCameraPositions.furnace(), 5 * SharedConstants.TICKS_PER_SECOND);
                                    scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 2), () -> {
                                        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.cook.additional"));
                                    }));
                                    activity.listen(FoodCookedEvent.EVENT, (item) -> {
                                       if (item.getItem().equals(Items.COOKED_CHICKEN) && !chickenCooked) {
                                           chickenCooked = true;
                                           tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.serve_order"));
                                           activity.listen(CustomerServedEvent.EVENT, (__customer) -> {
                                               orderServed = true;
                                               tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.order_served"));
                                               scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 5), () -> {
                                                   tutorial.endCurrentTutorial();
                                               }));
                                           });
                                       }
                                    });
                                }
                                return EventResult.PASS;
                            });
                        }));
                    }));
                }));
                return OrderTakenResult.allow(BaseOrder.inSeconds(Items.COOKED_CHICKEN, 10 * SharedConstants.TICKS_PER_MINUTE, 1));
            });
        });
    }
}
