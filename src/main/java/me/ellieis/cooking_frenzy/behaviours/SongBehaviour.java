package me.ellieis.cooking_frenzy.behaviours;

import me.ellieis.cooking_frenzy.CustomSounds;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongBehaviour extends BaseBehaviour {
    ArrayList<Song> songs = new ArrayList<>(List.of(
            new Song(2 * SharedConstants.TICKS_PER_MINUTE, CustomSounds.SONG1),
            new Song(99 * SharedConstants.TICKS_PER_SECOND, CustomSounds.SONG2),
            new Song(121 * SharedConstants.TICKS_PER_SECOND, CustomSounds.SONG3),
            new Song(112 * SharedConstants.TICKS_PER_SECOND, CustomSounds.SONG4),
            new Song(156 * SharedConstants.TICKS_PER_SECOND, CustomSounds.SONG5)
    ));
    long time = 0;
    long currentSongEndTime = 0;
    boolean stopped = false;
    boolean isWaiting;
    public SongBehaviour(GameSpace gameSpace, GameActivity activity, boolean debugMode, boolean isWaiting) {
        super(gameSpace, activity, debugMode);
        this.isWaiting = isWaiting;
    }

    @Override
    void setupEvents() {
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void playSong() {
        Collections.shuffle(songs);
        playSong(songs.getFirst());
    }

    private void playSong(Song song) {
        currentSongEndTime = time + song.time + 2 * SharedConstants.TICKS_PER_SECOND;
        gameSpace.getPlayers().playSound(SoundEvent.createFixedRangeEvent(song.identifier(), 1000), SoundSource.MUSIC, 1, 1);
    }

    private void playWaiting() {
        playSong(new Song(141 * SharedConstants.TICKS_PER_SECOND, CustomSounds.WAITING));
    }
    public void stopSongs() {
        for (Song song : songs) {
            stopped = true;
            gameSpace.getPlayers().sendPacket(new ClientboundStopSoundPacket(song.identifier(), SoundSource.MUSIC));
        }
    }

    private void onTick() {
        time++;
        if (currentSongEndTime <= time && !stopped) {
            if (isWaiting) {
                playWaiting();
            } else {
                playSong();
            }
        }
    }


    public record Song(int time, Identifier identifier) {

    }
}
