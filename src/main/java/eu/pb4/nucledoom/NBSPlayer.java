package eu.pb4.nucledoom;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.nucledoom.game.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.NoteBlock;
import net.raphimc.noteblocklib.data.MinecraftDefinitions;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.midi.MidiIo;
import net.raphimc.noteblocklib.format.nbs.NbsIo;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsSong;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.zip.ZipInputStream;

public class NBSPlayer implements DoomGame {
    private static final String USER_AGENT = "NucleDoom/NBS Player";
    @Nullable
    private final GameHandler handler;
    private final int[] pressNum = new int[9];
    private volatile boolean close = false;
    private Input input = Input.EMPTY;
    private int pressF;
    private int pressE;
    private int pressQ;
    private int pressSpace;
    private int pressShift;
    private int pressForward;
    private int pressBackward;
    private int pressLeft;
    private int pressRight;
    private CanvasImage screen;
    private float volume = 0.75f;
    private Player player;
    private CompletableFuture<Song> nextSong = null;


    public NBSPlayer(@Nullable GameHandler gameHandler,
                     @Nullable PlayerSaveData saveData,
                     DoomConfig config,
                     ResourceManager resourceManager) throws IOException {
        this.handler = gameHandler;
    }

    private static Song resolveNoteBlockWorldSong(String id) {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()) {

            var file = client.send(HttpRequest.newBuilder(URI.create("https://api.noteblock.world/api/v1/song/" + id + "/open"))
                    .header("src", "downloadButton")
                    .setHeader("User-Agent", USER_AGENT)
                    .build(), HttpResponse.BodyHandlers.ofString());

            var zip = new ZipInputStream(client.send(HttpRequest.newBuilder(URI.create(file.body()))
                    .header("src", "downloadButton")
                    .setHeader("User-Agent", USER_AGENT)
                    .build(), HttpResponse.BodyHandlers.ofInputStream()).body());


            NbsSong song = null;
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals("song.nbs")) {
                    song = NbsIo.readSong(zip, "song.nbs");
                    break;
                }
            }

            return song;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Song resolveDirectSong(String url) {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()) {
            var res = client.send(HttpRequest.newBuilder(URI.create(url))
                    .header("src", "downloadButton")
                    .setHeader("User-Agent", USER_AGENT)
                    .build(), HttpResponse.BodyHandlers.ofInputStream());

            if (url.endsWith(".nbs")) {
                return NbsIo.readSong(res.body(), url);
            } else if (url.endsWith(".mid") || url.endsWith(".midi")) {
                return MidiIo.readSong(res.body(), url);
            }


            throw new RuntimeException("Unsupported format");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onChat(String message) {
        if (message.startsWith("https://noteblock.world/song/")) {
            this.nextSong = CompletableFuture.supplyAsync(() -> resolveNoteBlockWorldSong(message.substring("https://noteblock.world/song/".length())));
            return true;
        } else if (message.startsWith("https://") || message.startsWith("http://")) {
            this.nextSong = CompletableFuture.supplyAsync(() -> resolveDirectSong(message));
            return true;
        }else if (message.startsWith("files:") && NucleDoom.IS_DEV) {
            try {
                this.nextSong = CompletableFuture.completedFuture(NbsIo.readSong(
                        Files.newInputStream(FabricLoader.getInstance().getGameDir().resolve(message.substring("files:".length()))), "song.nbs"));
            } catch (Throwable e) {

            }
            return true;
        } else if (message.startsWith("export:") && NucleDoom.IS_DEV && this.player != null) {

            try {
                var out = Files.newOutputStream(FabricLoader.getInstance().getGameDir().resolve(message.substring("export:".length())), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                NbsIo.writeSong((NbsSong) this.player.getSong(), out);
                out.close();

            } catch (Throwable e) {

            }
            return true;
        } else if (message.startsWith("dev:rebuild_sounds") && NucleDoom.IS_DEV) {
            SoundDecoder.load();
            return true;
        }

        return false;
    }

    @Override
    public void startGameLoop() throws Throwable {
        try {
            while (true) {
                if (this.nextSong != null) {
                    if (this.player != null) {
                        this.player.stop();
                        this.player = null;
                    }
                    if (this.nextSong.isCancelled() || this.nextSong.isCompletedExceptionally()) {
                        this.nextSong = null;
                    } else if (this.nextSong.isDone()) {
                        this.player = new Player(this.nextSong.get());
                        this.player.start();
                        this.nextSong = null;
                    }
                }

                this.drawFrame();
                if (this.close) break;
                Thread.sleep(1000 / 30);
            }
        } finally {
            if (this.player != null) {
                this.player.stop();
            }
        }
    }

    @Override
    public void clear() {
        if (this.player != null) {
            this.player.stop();
        }
        if (this.nextSong != null) {
            this.nextSong.cancel(true);
            this.nextSong = null;
        }

        this.close = true;
    }

    public void drawFrame() {
        if (this.close) {
            throw new GameClosed(0);
        }

        if (this.handler == null) {
            return;
        }

        if (this.screen == null) {
            this.screen = new CanvasImage(320, 200);
        }
        CanvasUtils.clear(this.screen, CanvasColor.WHITE_GRAY_HIGH);
        int size = 2;
        int pan = 100 / size;
        int vpan = 75 * 2 / size;

        var playerDepBackground = this.player != null ? CanvasColor.WHITE_HIGH : CanvasColor.WHITE_NORMAL;

        CanvasUtils.fill(this.screen, this.screen.getWidth() / 2 - pan * size - 2, 32 - 2, this.screen.getWidth() / 2 + pan * size + 2, 32 + vpan * size + 2, CanvasColor.WHITE_GRAY_LOWEST);
        if (this.player != null && this.player.getSong() instanceof NbsSong) {
            CanvasUtils.fill(this.screen, this.screen.getWidth() / 2 - pan * size - 1, 32 - 1, this.screen.getWidth() / 2 + pan * size + 1, 32 + vpan * size + 1, CanvasColor.WHITE_HIGH);
            CanvasUtils.fill(this.screen, this.screen.getWidth() / 2, 32 - 1, this.screen.getWidth() / 2 + 1, 32 + vpan * size + 1, CanvasColor.WHITE_NORMAL);
        } else {
            CanvasUtils.fill(this.screen, this.screen.getWidth() / 2 - pan * size - 1, 32 - 1, this.screen.getWidth() / 2 + pan * size + 1, 32 + vpan * size + 1, CanvasColor.WHITE_NORMAL);
        }

        var font = DefaultFonts.VANILLA;
        var fontSize = 8;
        var name = "---";
        var author = "---";

        var time = "00:00";
        var endTime = "00:00";
        float progress = 0;
        if (this.player != null) {
            var pos = this.player.getTick();

            var song = this.player.getSong();

            name = song.getTitleOrFileName().replace('\n', ' ');
            author = song.getAuthorOr("-----");
            var og = song.getOriginalAuthorOr("");
            if (!og.isEmpty()) {
                author += " (Original: " + og + ")";
            }
            var timeSeconds = this.player.getMillisecondPosition() / 1000;
            var endTimeSeconds = song.getLengthInMilliseconds() / 1000;
            time = String.format("%02d:%02d", timeSeconds / 60, (timeSeconds % 60));
            endTime = String.format("%02d:%02d", endTimeSeconds / 60, (endTimeSeconds % 60));

            progress = this.player.getMillisecondPosition() / (float) song.getLengthInMilliseconds();

            if (song instanceof NbsSong nbsSong) {
                var sorted = new ArrayList<>(nbsSong.getLayers().entrySet());
                for (int i = 0; i < sorted.size(); ) {
                    var layer = sorted.get(i).getValue();
                    if (layer.getNotes().isEmpty() || layer.getVolume() == 0) {
                        sorted.remove(i);
                    } else {
                        i++;
                    }
                }
                sorted.sort(Map.Entry.comparingByKey());

                var y = 0;
                for (var pair : sorted) {
                    if (y > vpan) {
                        continue;
                    }
                    for (var tick = -pan; tick < pan; tick++) {
                        var note = pair.getValue().getNotes().get(tick + pos);
                        if (note == null) continue;
                        int color;

                        {
                            var mcNote = NoteBlock.getPitchFromNote(note.getKey());
                            var red = Math.max(0.0F, Mth.sin((mcNote + 0.0F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
                            var green = Math.max(0.0F, Mth.sin((mcNote + 0.33333334F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
                            var blue = Math.max(0.0F, Mth.sin((mcNote + 0.6666667F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
                            color = ARGB.colorFromFloat(0, red, green, blue);
                        }

                        for (int x2 = this.screen.getWidth() / 2 + tick * size; x2 < this.screen.getWidth() / 2 + (tick + 1) * size; x2++) {
                            for (int y2 = 32 + y * size; y2 < 32 + (y + 1) * size; y2++) {
                                this.screen.set(x2, y2, CanvasUtils.findClosestColor(ARGB.scaleRGB(color, 255 - (y2 % 2 + x2 % 2) * 64)));
                            }
                        }
                    }
                    y++;
                }
            }
        }


        var y = 2;
        var nameFontSize = 16;
        CanvasFont nameFont = DefaultFonts.UNIFONT;
        var nameSize = nameFont.getTextWidth(name, nameFontSize);
        if (nameSize > this.screen.getWidth()) {
            nameFontSize = 8;
            font = DefaultFonts.VANILLA;
            nameSize = nameFont.getTextWidth(name, nameFontSize);
            if (nameSize > this.screen.getWidth()) {
                nameFont = ExtraFonts.OPEN_ZOO_4x8;
                nameSize = nameFont.getTextWidth(name, nameFontSize);
            }
        }

        nameFont.drawText(this.screen, name, (this.screen.getWidth() - nameSize) / 2 + 1, y + 1, nameFontSize, CanvasColor.LIGHT_GRAY_HIGH);
        nameFont.drawText(this.screen, name, (this.screen.getWidth() - nameSize) / 2, y, nameFontSize, CanvasColor.BLACK_HIGH);
        y += nameFontSize + 2;

        nameFontSize = 8;
        nameFont = font;
        nameSize = nameFont.getTextWidth(author, nameFontSize);
        if (nameSize + 8 > this.screen.getWidth()) {
            nameFont = ExtraFonts.OPEN_ZOO_4x8;
        }

        nameFont.drawText(this.screen, author, 8 + 1, y + 1, nameFontSize, CanvasColor.WHITE_GRAY_NORMAL);
        nameFont.drawText(this.screen, author, 8, y, nameFontSize, CanvasColor.GRAY_HIGH);

        {
            var centerX = (this.screen.getWidth() / 2 + pan * size + 1 + this.screen.getWidth()) / 2;
            var vol = (Mth.ceil(this.volume * 100)) + "%";

            font.drawText(this.screen, vol, centerX - font.getTextWidth(vol, fontSize) / 2, 165, fontSize, CanvasColor.BLACK_HIGH);
            ExtraFonts.OPEN_ZOO_4x6.drawText(this.screen, "Volume", centerX - ExtraFonts.OPEN_ZOO_4x6.getTextWidth("Volume", 8) / 2, 102, 8, CanvasColor.WHITE_GRAY_LOWEST);

            CanvasUtils.fill(this.screen, centerX - 5 - 2, 110 - 2, centerX + 5 + 2, 160 + 2, CanvasColor.WHITE_GRAY_LOWEST);
            CanvasUtils.fill(this.screen, centerX - 5 - 1, 110 - 1, centerX + 5 + 1, 160 + 1, CanvasColor.WHITE_HIGH);
            CanvasUtils.fill(this.screen, centerX - 5, Mth.lerpDiscrete(this.volume,  160, 110),  centerX + 5, 160, CanvasColor.BLACK_HIGH);
        }

        font.drawText(this.screen, time, 16, 190 - 4 - 8, 8, CanvasColor.BLACK_HIGH);
        font.drawText(this.screen, endTime, this.screen.getWidth() - 16 - font.getTextWidth(endTime, 8), 190 - 4 - 8, 8, CanvasColor.BLACK_HIGH);


        CanvasUtils.fill(this.screen, 16 - 2, 190 - 2, this.screen.getWidth() - 16 + 2, 194 + 2, CanvasColor.WHITE_GRAY_LOWEST);
        CanvasUtils.fill(this.screen, 16 - 1, 190 - 1, this.screen.getWidth() - 16 + 1, 194 + 1, playerDepBackground);

        CanvasUtils.fill(this.screen, 16, 190, (int) (16 + (this.screen.getWidth() - 32) * progress), 194, CanvasColor.BLACK_HIGH);

        this.handler.getCanvas().drawFrame(this.screen);
    }

    @Override
    public void updateKeyboard(Input input) {
        if (input.forward()) {
            this.pressForward = 10;
            this.volume = Mth.clamp(this.volume + 0.01f, 0, 1);
        }

        if (input.backward()) {
            this.pressBackward = 10;
            this.volume = Mth.clamp(this.volume - 0.01f, 0, 1);
        }

        if (input.jump() && !this.input.jump() && this.player != null) {
            if (!this.player.isRunning()) {
                if (!this.player.isPaused()) {
                    this.player.start(0, this.player.getMillisecondPosition() >= this.player.getSong().getLengthInMilliseconds() ? 0 : this.player.getTick());
                }
                this.player.setPaused(false);
            } else {
                this.player.setPaused(!this.player.isPaused());
            }
        }

        this.input = input;
    }

    @Override
    public void updateMouse(float xDelta, float yDelta, boolean mouseLeft) {


    }

    @Override
    public void selectSlot(int selectedSlot) {
        //if (selectedSlot == 7) {
        //    this.doom.graphicSystem.setUsegamma(this.doom.graphicSystem.getUsegamma() + 1);
        //    return;
        //}


        this.pressNum[selectedSlot] = 2;
    }

    @Override
    public void pressE() {
        this.pressE = 5;
    }

    @Override
    public void pressQ() {
        this.pressQ = 5;
    }

    @Override
    public void pressF() {
        this.pressF = 5;
    }

    @Override
    public void tick() {
        if (--this.pressForward < 0 && this.input.forward()) {
            this.pressForward = 2;
            this.volume = Mth.clamp(this.volume + 0.01f, 0, 1);
        } else if (--this.pressBackward < 0 && this.input.backward()) {
            this.pressBackward = 2;
            this.volume = Mth.clamp(this.volume - 0.01f, 0, 1);
        }

        int spring = this.input.sprint() ? 10 : 2;
        if (this.input.left() && this.player != null) {
            this.player.setTick(Mth.clamp(this.player.getTick() - spring, 0, this.player.getSong().millisecondsToTick(this.player.getSong().getLengthInMilliseconds())));
        }

        if (this.input.right() && this.player != null) {
            this.player.setTick(Mth.clamp(this.player.getTick() + spring, 0, this.player.getSong().millisecondsToTick(this.player.getSong().getLengthInMilliseconds())));
        }


        if (--pressF == 0) {

        }

        if (--pressE == 0) {

        }

        if (--pressQ == 0) {

        }

        for (int i = 0; i < 9; i++) {
            if (--this.pressNum[i] == 0) {

            }
        }

        if (NucleDoom.IS_DEV) {

        }
    }

    @Override
    public void extractAudio(BiConsumer<String, byte[]> consumer) {
    }

    public void playSound(SoundTarget target, SoundEvent soundVanilla, float pitch, float volume, long seed) {
        if (this.handler != null) {
            this.handler.playSound(target, soundVanilla, pitch, volume, seed);
        }
    }

    public boolean supportsSoundTarget(SoundTarget target) {
        return this.handler != null && this.handler.supportsSoundTargets(target);
    }

    @Override
    public String getControls() {
        return """
                  ## Controls
                  - W - Volume UP
                  - S - Volume DOWN
                  - D - Go Forwards
                  - A - Go Backwards
                  - Space - Pause/Play
                  - Chat - Link to song
                """;
    }

    private final class Player extends SongPlayer {
        public Player(Song song) {
            super(song);
        }

        @Override
        protected void playNotes(List<Note> list) {
            var tmpNote = new Note();
            var nList = new ArrayList<>(list);
            nList.sort(Comparator.comparingDouble(Note::getVolume).reversed());
            for (var note : nList) {
                if (note.getVolume() < 0.001) continue;

                if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                    if (note.isOutsideMinecraftOctaveRange()) {
                        if (supportsSoundTarget(SoundTarget.MUSIC_VANILLA)) {
                            tmpNote.setInstrument(instrument);
                            tmpNote.setMidiKey(note.getMidiKey());
                            tmpNote.setVolume(note.getVolume());
                            MinecraftDefinitions.instrumentShiftNote(tmpNote);
                            MinecraftDefinitions.clampNoteKey(tmpNote);
                            playSound(SoundTarget.MUSIC_VANILLA,
                                    BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(((MinecraftInstrument) tmpNote.getInstrument()).mcSoundName())),
                                    tmpNote.getPitch(), volume * tmpNote.getVolume(), 0);
                        }
                        if (supportsSoundTarget(SoundTarget.MUSIC_EXT)) {
                            tmpNote.setInstrument(instrument);
                            tmpNote.setMidiKey(note.getMidiKey());
                            tmpNote.setVolume(note.getVolume());
                            var suffix = MinecraftDefinitions.applyExtendedNotesResourcePack(tmpNote);
                            playSound(SoundTarget.MUSIC_EXT,
                                    new net.minecraft.sounds.SoundEvent(Identifier.tryParse(instrument.mcSoundName() + "_" + suffix), Optional.empty()),
                                    tmpNote.getPitch(), volume * tmpNote.getVolume(), 0);
                        }
                    } else {
                        playSound(SoundTarget.MUSIC_ANY, BuiltInRegistries.SOUND_EVENT.getValue(Identifier.tryParse(instrument.mcSoundName())), note.getPitch(), volume * note.getVolume(), 0);
                    }
                } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                    var decoded = SoundDecoder.decode(instrument.getSoundFilePath());
                    if (decoded != null) {
                        playSound(SoundTarget.MUSIC_ANY, decoded.event(), note.getPitch() / decoded.pitch(), volume * note.getVolume() / decoded.volume(), decoded.seed());
                    } else {
                        playSound(SoundTarget.MUSIC_ANY, SoundEvent.createVariableRangeEvent(Identifier.tryParse(instrument.getSoundFilePath())), note.getPitch(), volume * note.getVolume(), 0);
                    }
                }
            }
        }
    }
}
