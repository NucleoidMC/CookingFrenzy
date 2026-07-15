package me.ellieis.cooking_frenzy.behaviours.extra;

import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset;
import me.ellieis.cooking_frenzy.behaviours.CustomerBehaviour;
import me.ellieis.cooking_frenzy.behaviours.FarmingBehaviour;
import me.ellieis.cooking_frenzy.behaviours.TutorialBehaviour;
import me.ellieis.cooking_frenzy.events.*;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;

public class CraftingTutorial extends BaseTutorial {
    CustomerBehaviour<Active> customerBehaviour;
    FarmingBehaviour farmingBehaviour;
    boolean customerSat = false;
    boolean orderTaken = false;
    boolean checkForFarming = false;
    boolean checkingForItemsBought = false;
    int seedsBought = 0;
    int hoeBought = 0;
    int bonemealBought = 0;
    boolean messagesSent = false;
    boolean wheatPlanted = false;
    int wheatObtained = 0;
    boolean breadCrafted = false;
    boolean orderServed = false;
    boolean stallingCamera = false;
    public CraftingTutorial(TutorialBehaviour tutorial, GameActivity activity, GameSpace gameSpace, Active map, Scheduler scheduler, CookingFrenzyActive game, CustomerBehaviour<Active> customerBehaviour, FarmingBehaviour farmingBehaviour) {
        super(tutorial, activity, gameSpace, map, scheduler, game);
        this.customerBehaviour = customerBehaviour;
        this.farmingBehaviour = farmingBehaviour;
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void onTick() {
        if (stallingCamera) {
            tutorial.timeUntilAngleEnds = 999;
        }
        if (checkingForItemsBought) {
            for (ServerPlayer player : gameSpace.getPlayers().participants()) {
                player.sendSystemMessage(
                        Component.object(new AtlasSprite(AtlasIds.ITEMS, Identifier.parse("item/wheat_seeds"))).append(": ")
                                .append(seedsBought > 0 ? Component.literal("✓ ").withStyle(ChatFormatting.GREEN) : Component.literal("X ").withStyle(ChatFormatting.RED))
                                .append(Component.object(new AtlasSprite(AtlasIds.ITEMS, Identifier.parse("item/wooden_hoe")))).append(": ").withStyle(ChatFormatting.WHITE)
                                .append(hoeBought > 0 ? Component.literal("✓").withStyle(ChatFormatting.GREEN) : Component.literal("X").withStyle(ChatFormatting.RED)), true);
            }
        }
        if (checkForFarming) {
            for (ServerPlayer player : this.gameSpace.getPlayers().participants()) {
                if (map.isInFarmingArea(player)) {
                    checkForFarming = false;
                    this.farmingBehaviour.glowMinecart(false);
                    this.farmingBehaviour.glowFarmer(true);
                    this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.trader"));
                    checkingForItemsBought = true;
                    activity.listen(ItemBuyEvent.EVENT, (itemStack) -> {
                        if (checkingForItemsBought) {
                            Item item = itemStack.getItem();
                            if (item.equals(Items.WOODEN_HOE)) {
                                hoeBought++;
                            } else if (item.equals(Items.WHEAT_SEEDS)) {
                                seedsBought++;
                            } else if (item.equals(Items.BONE_MEAL)) {
                                bonemealBought++;
                            }
                            if (hoeBought >= 1 && seedsBought >= 1) {
                                if (messagesSent) {
                                    checkingForItemsBought = false;
                                    this.farmingBehaviour.glowFarmer(false);
                                    return;
                                }
                                this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.plant"));
                                this.activity.listen(BlockUseEvent.EVENT, (_player, hand, hitResult) -> {
                                    if (this.game.level.getBlockState(hitResult.getBlockPos()).getBlock().equals(Blocks.FARMLAND) && _player.getItemInHand(hand).getItem().equals(Items.WHEAT_SEEDS)) {
                                        wheatPlanted = true;
                                        if (bonemealBought == 0) {
                                            this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.trader.bone_meal"));
                                        }
                                    }
                                    return InteractionResult.PASS;
                                });
                                this.activity.listen(BlockBreakEvent.EVENT, (_player, level, pos) -> {
                                    if (level.getBlockState(pos).getBlock().equals(Blocks.WHEAT)) {
                                        wheatObtained++;
                                        if (wheatObtained == 1) {
                                            this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.more_wheat"));
                                        } else if (wheatObtained == 3) {
                                            this.tutorial.setCameraAngle(this.map.tutorialCameraPositions.crafter(), (SharedConstants.TICKS_PER_SECOND * 5));
                                            this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.finished"));
                                            this.scheduler.addTask(new Task(this.game.time + (SharedConstants.TICKS_PER_SECOND * 10), () -> {
                                                this.tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.cook.additional"));
                                            }));
                                            activity.listen(ItemCraftedEvent.EVENT, (_item) -> {
                                               if (_item.getItem().equals(Items.BREAD) && !breadCrafted) {
                                                   breadCrafted = true;
                                                   tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.serve_order"));
                                                   activity.listen(CustomerServedEvent.EVENT, (customer) -> {
                                                       orderServed = true;
                                                       tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.order_served"));
                                                       scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 5), () -> {
                                                           tutorial.endCurrentTutorial();
                                                       }));
                                                   });
                                               }
                                            });
                                        }
                                    }
                                    return EventResult.PASS;
                                });
                                messagesSent = true;
                            }
                        }
                    });
                }
            }
        }
    }

    /*
    tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.cook"));
                                    tutorial.setCameraAngle(map.tutorialCameraPositions.furnace(), 5 * SharedConstants.TICKS_PER_SECOND);
                                    scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 2), () -> {
                                        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.cook.additional"));
                                    }));
                                    activity.listen(FoodCookedEvent.EVENT, (item) -> {
                                        if (item.getItem().equals(Items.COOKED_CHICKEN) && !chickenCooked) {
                                            breadCrafted = true;
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
     */
    public void startTutorial() {
        stallingCamera = true;
        tutorial.setCameraAngle(map.tutorialCameraPositions.customer(), 999);
        customerBehaviour.spawnCustomer(false);
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
                tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.order_taken", Items.BREAD.getDefaultInstance().getItemName()));
                scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 4), () -> {
                    tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.outside"));
                    tutorial.setCameraAngle(map.tutorialCameraPositions.farmingOutside(), 5 * SharedConstants.TICKS_PER_SECOND);
                    this.scheduler.addTask(new Task(this.game.time + (SharedConstants.TICKS_PER_SECOND * 5), () -> {
                        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.farming.go"));
                        farmingBehaviour.glowMinecart(true);
                    }));
                    checkForFarming = true;
                }));
                return OrderTakenResult.allow(BaseOrder.inSeconds(Items.BREAD, 10 * SharedConstants.TICKS_PER_MINUTE, 1));
            });
        });
    }
}
