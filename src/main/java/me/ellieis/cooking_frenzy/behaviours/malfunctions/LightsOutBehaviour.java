package me.ellieis.cooking_frenzy.behaviours.malfunctions;

import me.ellieis.cooking_frenzy.CustomSounds;
import me.ellieis.cooking_frenzy.behaviours.BaseBehaviour;
import me.ellieis.cooking_frenzy.behaviours.DisableableBehaviour;
import me.ellieis.cooking_frenzy.behaviours.extra.PressurePlateReader;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LightsOutBehaviour extends BaseBehaviour implements MalfunctionBehaviour {
    CookingFrenzyActive game;
    boolean lightsAreOn = true;
    ArrayList<ServerPlayer> playersThatWalkedInTheVeryImportantRoomThatIsTheBreakerPanel = new ArrayList<>();
    List<TemplateRegion> pressurePlates;
    List<BlockPos> breakerDoors = new ArrayList<>();
    List<TemplateRegion> lightsArea;
    List<TemplateRegion> breakerPanels;
    ServerLevel level;
    ArrayList<ArrayList<Boolean>> breakerCode = new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    boolean isSourceLeft = false;
    public LightsOutBehaviour(GameSpace gameSpace, GameActivity activity, CookingFrenzyActive game) {
        super(gameSpace, activity, game.debugMode);
        this.game = game;
        this.level = game.level;
        this.pressurePlates = game.map.getLightBreakerPlates();
        for (TemplateRegion breakerDoor : game.map.getBreakerDoors()) {
            breakerDoors.add(BlockPos.containing(breakerDoor.getBounds().center()));
        }
        this.lightsArea = game.map.getLightsArea();
        this.breakerPanels = game.map.getLightsBreaker();
    }

    public Component getTitle() {
        return Component.translatable("cooking_frenzy.malfunctions.lights_out");
    }

    public Component getDesc() {
        return Component.translatable("cooking_frenzy.malfunctions.lights_out.desc");
    }

    @Override
    protected void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
        if (this.debugMode) {
            activity.listen(PlayerChatEvent.EVENT, this::onChat);
        }
    }

    private BlockPos getInitialPanelPos(ArrayList<TemplateRegion> breakerLights, int row, boolean isLeftPanel) {
        BlockPos initialPos = null;
        for (TemplateRegion breakerLight : breakerLights) {
            if (breakerLight.getData().getIntOr("row", 0) == row) {
                if (isLeftPanel) {
                    for (BlockPos bound : breakerLight.getBounds()) {
                        if (initialPos == null) {
                            initialPos = bound.immutable();
                            continue;
                        }
                        if (bound.getZ() < initialPos.getZ()) {
                            initialPos = bound.immutable();
                        }
                    }
                } else {
                    for (BlockPos bound : breakerLight.getBounds()) {
                        if (initialPos == null) {
                            initialPos = bound.immutable();
                            continue;
                        }
                        if (bound.getZ() > initialPos.getZ()) {
                            initialPos = bound.immutable();
                        }
                    }
                }
                break;
            }
        }
        return initialPos;
    }

    private boolean arePanelsIdentical(boolean isSourceLeftPanel) {
        return readPanel(isSourceLeftPanel).equals(readPanel(!isSourceLeftPanel));
    }

    private ArrayList<ArrayList<Boolean>> readPanel(boolean isLeftPanel) {
        ArrayList<ArrayList<Boolean>> readCode = new ArrayList<>();
        ArrayList<TemplateRegion> breakerLights = new ArrayList<>(breakerPanels.stream().filter(region -> region.getData().getBooleanOr((isLeftPanel ? "left" : "right"), false)).toList());
        for (int i = 0; i < 3; i++) {
            ArrayList<Boolean> row = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                BlockPos pos = getInitialPanelPos(breakerLights, i, isLeftPanel).offset(0, 0, (isLeftPanel) ? j : -j);
                row.add(j, level.getBlockState(pos).getValue(BlockStateProperties.LIT));
            }
            readCode.add(i, row);
        }
        return readCode;
    }

    private void writeToPanel(boolean isLeftPanel, boolean isMain) {
        ArrayList<TemplateRegion> breakerLights = new ArrayList<>(breakerPanels.stream().filter(region -> region.getData().getBooleanOr((isLeftPanel ? "left" : "right"), false)).toList());
        for (int i = 0; i < 3; i++) {
            ArrayList<Boolean> row = this.breakerCode.get(i);
            for (int j = 0; j < 3; j++) {
                boolean val = row.get(j);
                BlockPos pos = getInitialPanelPos(breakerLights, i, isLeftPanel).offset(0, 0, (isLeftPanel) ? j : -j);
                level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, val), 2);
                BlockPos offsetPos = pos.offset((isLeftPanel) ? -1 : 1, 0, 0);
                level.setBlock(offsetPos, level.getBlockState(offsetPos).setValue(BlockStateProperties.POWERED, isMain), 2);
            }
        }
    }

    private void generateCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (ArrayList<Boolean> booleans : breakerCode) {
            booleans.clear();
            for (int i = 0; i < 3; i++) {
                booleans.add(i, random.nextFloat() >= 0.5);
            }
        }
        writeToPanel(!(random.nextFloat() >= 0.5), true);
    }

    private void onTick() {
        if (!this.lightsAreOn) {
            for (ServerPlayer player : gameSpace.getPlayers().participants()) {
                for (TemplateRegion templateRegion : lightsArea) {
                    if (templateRegion.getBounds().contains(player.blockPosition())) {
                        if (!playersThatWalkedInTheVeryImportantRoomThatIsTheBreakerPanel.contains(player)) {
                            playersThatWalkedInTheVeryImportantRoomThatIsTheBreakerPanel.add(player);
                            PlayerUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.UI, 1, 1);
                            player.sendSystemMessage(Component.translatable("cooking_frenzy.malfunctions.lights_out.explanation"));
                        }
                        if (!this.game.isSinglePlayer) {
                            BlockPos breakerDoor = null;
                            for (BlockPos door : breakerDoors) {
                                if (breakerDoor == null) {
                                    breakerDoor = door.immutable();
                                } else {
                                    if (breakerDoor.distManhattan(player.blockPosition()) > door.distManhattan(player.blockPosition())) {
                                        breakerDoor = door.immutable();
                                    }
                                }
                            }
                            level.setBlock(breakerDoor, level.getBlockState(breakerDoor).setValue(DoorBlock.OPEN, false), 2);
                        }
                    }
                }
            }
            PressurePlateReader.updateSigns(pressurePlates, level, "cooking_frenzy.malfunctions.lights_out.sign.top", "cooking_frenzy.malfunctions.lights_out.sign.bottom", false);
            if (PressurePlateReader.getPoweredPlates(pressurePlates, level) >= pressurePlates.size()) {
                for (BlockPos breakerDoor : breakerDoors) {
                    if (!level.getBlockState(breakerDoor).getValue(DoorBlock.OPEN)) {
                        level.setBlock(breakerDoor, level.getBlockState(breakerDoor).setValue(DoorBlock.OPEN, true), 2);
                        level.playSound(null, breakerDoor, BlockSetType.IRON.doorOpen(), SoundSource.BLOCKS);
                    }
                }
            }
            if (arePanelsIdentical(isSourceLeft)) {
                this.toggleMalfunction(true);
            }
        }
    }

    private EventResult onChat(ServerPlayer player, PlayerChatMessage playerChatMessage, ChatType.Bound bound) {
        if (playerChatMessage.decoratedContent().getString().equals("lights")) {
            toggleMalfunction(!lightsAreOn);
        }
        return EventResult.PASS;
    }

    public void toggleMalfunction(boolean val) {
        this.lightsAreOn = val;
        for (TemplateRegion light : game.map.getLights()) {
            BlockPos pos = BlockPos.containing(light.getBounds().center());
            ServerLevel level = game.level;
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, val), 2);
        }
        for (BlockPos breakerDoor : breakerDoors) {
            level.setBlock(breakerDoor, level.getBlockState(breakerDoor).setValue(DoorBlock.OPEN, val), 2);
            level.playSound(null, breakerDoor, BlockSetType.IRON.doorClose(), SoundSource.BLOCKS);
        }
        if (!lightsAreOn) {
            gameSpace.getPlayers().playSound(SoundEvent.createVariableRangeEvent(CustomSounds.POWER_OUT), SoundSource.AMBIENT, 1, 1);
            generateCode();
        } else {
            gameSpace.getPlayers().playSound(SoundEvent.createVariableRangeEvent(CustomSounds.POWER_ON), SoundSource.AMBIENT, 1, 1);
            this.game.scheduler.addTask(new Task(game.time + 5 * SharedConstants.TICKS_PER_SECOND, () -> {
                for (ArrayList<Boolean> booleans : breakerCode) {
                    booleans.clear();
                    for (int i = 0; i < 3; i++) {
                        booleans.add(i, false);
                    }
                }
                writeToPanel(true, false);
                writeToPanel(false, false);
            }));
        }

        for (DisableableBehaviour disableableBehaviour : game.getDisableableBehaviours(MalfunctionType.LIGHTS)) {
            if (val) {
                disableableBehaviour.enableBehaviour(MalfunctionType.LIGHTS);
            } else {
                disableableBehaviour.disableBehaviour(MalfunctionType.LIGHTS);
            }
        }
    }
}
