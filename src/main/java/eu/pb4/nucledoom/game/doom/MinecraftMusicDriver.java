package eu.pb4.nucledoom.game.doom;

import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.midi.MidiIo;
import net.raphimc.noteblocklib.format.midi.model.MidiSong;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import org.jetbrains.annotations.Nullable;
import s.IMusic;
import s.MusReader;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class MinecraftMusicDriver implements IMusic {
    private final DoomGame game;
    private float volume;

    private Player player;

    public MinecraftMusicDriver(DoomGame game) {

        this.game = game;
    }

    @Override
    public void InitMusic() {
    }

    @Override
    public void ShutdownMusic() {
        if (this.player != null) {
            this.player.stop();
        }
        this.player = null;
    }

    @Override
    public void SetMusicVolume(int i) {
        this.volume = i / 100f;
    }

    @Override
    public void PauseSong(int i) {
        if (this.player != null) {
            this.player.setPaused(true);
        }
    }
    @Override
    public void ResumeSong(int i) {
        if (this.player != null) {
            this.player.setPaused(false);
        }
    }

    @Override
    public void PlaySong(int i, boolean b) {
        if (this.player != null) {
            this.player.loop = b;
            this.player.start();
        }
    }

    @Override
    public void StopSong(int i) {
        if (this.player != null) {
            this.player.loop = false;
            this.player.stop();
        }
    }

    @Override
    public void UnRegisterSong(int i) {
        if (this.player != null) {
            this.player.loop = false;
            this.player.stop();
        }
        this.player = null;
    }

    @Override
    public int RegisterSong(byte[] data) {
        try {
            if (data[0] == 'M' && data[1] == 'U' && data[2] == 'S') {
                var sequence1 = MusReader.getSequence(new ByteArrayInputStream(data));
                var sequence = new Sequence(0, 14 * 30 / 2, 1);
                var track1  = sequence1.getTracks()[0];
                var track2  = sequence.getTracks()[0];
                for (int i = 0; i < track1.size(); i++) {
                    track2.add(track1.get(i));
                }

                var tmp = new ByteArrayOutputStream();
                MidiSystem.write(sequence, 0, tmp);
                data = tmp.toByteArray();
            }

            this.player = new Player(MidiIo.readSong(new ByteArrayInputStream(data), ""));
        } catch (Exception e) {
            return -1;
        }
        // In good old C style, we return 0 upon success?
        return 0;
    }
    private final class Player extends SongPlayer {
        public boolean loop;

        public Player(Song song) {
            super(song);
        }

        @Override
        protected void playNotes(List<Note> list) {
            for (var note : list) {
                SoundEvent event = SoundEvents.INTENTIONALLY_EMPTY;
                if (note.getInstrument() instanceof MinecraftInstrument minecraftInstrument) {
                    event = Registries.SOUND_EVENT.get(Identifier.of(minecraftInstrument.mcSoundName()));
                } else if (note.getInstrument() instanceof NbsCustomInstrument customInstrument) {
                    event = new SoundEvent(Identifier.of(customInstrument.getName()), Optional.empty());
                }

                game.playSound(event, note.getPitch(), note.getVolume() * volume);
            }
        }

        @Override
        protected void onFinished() {
            super.onFinished();
            if (this.loop) {
                this.start();
            }
        }
    }
}
