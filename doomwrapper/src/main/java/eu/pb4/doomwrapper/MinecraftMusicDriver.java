package eu.pb4.doomwrapper;

import eu.pb4.nucledoom.game.SoundTarget;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.data.MinecraftDefinitions;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.midi.MidiIo;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import s.IMusic;
import s.MusReader;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class MinecraftMusicDriver implements IMusic {
    private final DoomGameImpl game;
    private float volume;

    private Player player;

    public MinecraftMusicDriver(DoomGameImpl game) {

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
            Song song;
            if (data[0] == 'M' && data[1] == 'U' && data[2] == 'S') {
                var sequence1 = MusReader.getSequence(new ByteArrayInputStream(data));
                song = MidiIo.parseSong(sequence1, "");
            } else {
                song = MidiIo.readSong(new ByteArrayInputStream(data), "");
            }

            /*Files.deleteIfExists(Path.of("doom.nbs"));
            var song2 = NoteBlockLib.convertSong(song, SongFormat.NBS);
            song2.getNotes().forEach(MinecraftDefinitions::instrumentShiftNote);
            song2.getNotes().forEach(MinecraftDefinitions::clampNoteKey);
            NoteBlockLib.writeSong(song2, Path.of("doom.nbs"));*/

            this.player = new Player(song);
        } catch (Exception e) {
            e.printStackTrace();
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
            var tmpNote = new Note();
            for (var note : list) {
                if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                    if (note.isOutsideMinecraftOctaveRange()) {
                        if (game.supportsSoundTarget(SoundTarget.MUSIC_VANILLA)) {
                            tmpNote.setInstrument(instrument);
                            tmpNote.setMidiKey(note.getMidiKey());
                            tmpNote.setVolume(note.getVolume());
                            MinecraftDefinitions.instrumentShiftNote(tmpNote);
                            MinecraftDefinitions.clampNoteKey(tmpNote);
                            game.playSound(SoundTarget.MUSIC_VANILLA,
                                    Registries.SOUND_EVENT.get(Identifier.of(((MinecraftInstrument) tmpNote.getInstrument()).mcSoundName())),
                                    tmpNote.getPitch(), volume * tmpNote.getVolume());
                        }
                        if (game.supportsSoundTarget(SoundTarget.MUSIC_EXT)) {
                            tmpNote.setInstrument(instrument);
                            tmpNote.setMidiKey(note.getMidiKey());
                            tmpNote.setVolume(note.getVolume());
                            var suffix = MinecraftDefinitions.applyExtendedNotesResourcePack(tmpNote);
                            game.playSound(SoundTarget.MUSIC_EXT,
                                    new SoundEvent(Identifier.of(instrument.mcSoundName() + "_" + suffix), Optional.empty()),
                                    tmpNote.getPitch(), volume * tmpNote.getVolume());
                        }
                    } else {
                        game.playSound(SoundTarget.MUSIC_ANY, Registries.SOUND_EVENT.get(Identifier.of(instrument.mcSoundName())), note.getPitch(), volume * note.getVolume());
                    }
                }
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
