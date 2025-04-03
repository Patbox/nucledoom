package eu.pb4.doomwrapper;

import data.sounds;
import doom.*;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.PlayerSaveData;
import eu.pb4.nucledoom.game.*;
import g.Signals;
import i.DoomSystem;
import m.Menu;
import m.MenuRoutine;
import mochadoom.SystemHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.PlayerInput;
import org.jetbrains.annotations.Nullable;
import utils.C2JUtils;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DoomGameImpl implements DoomGame {
    private final DoomMain<?, ?> doom;
    private final CVarManager cvar;
    private final DoomConfig config;
    @Nullable
    private final GameCanvas canvas;
    private final ConfigManager configManager;
    private final ResourceManager resource;
    private final Map<String, Supplier<InputStream>> resourceCache = new HashMap<>();
    private final byte[] wadData;
    private final Map<String, byte[]> iwadData = new HashMap<>();
    private final String wadName;

    private volatile boolean close = false;
    private PlayerInput input = PlayerInput.DEFAULT;
    private int pressF;
    private int pressE;
    private int pressQ;

    private final FastCanvasImage screen;

    private final int[] pressNum = new int[9];
    private final event_t.mouseevent_t mouseEvent = new event_t.mouseevent_t(evtype_t.ev_mouse, 0, 0, 0);


    public DoomGameImpl(@Nullable GameCanvas gameCanvas,
                        @Nullable PlayerSaveData saveData,
                        DoomConfig config,
                        ResourceManager resourceManager,
                        int scale) throws IOException {
        SoundMap.updateSoundMap();
        this.canvas = gameCanvas;
        this.resource = resourceManager;
        this.config = config;
        this.wadName = this.config.wadName().toLowerCase(Locale.ROOT) + ".wad";
        this.wadData = NucleDoom.WADS.get(this.config.wadFile());
        SystemHandler.instance = new NucleSystemHandler(this, saveData);
        var cvars = new ArrayList<String>();
        cvars.addAll(List.of("-multiply", String.valueOf(scale), "-novolatileimage", "-hidediskdrawer", "-iwad", this.wadName));
        if (!config.pwads().isEmpty()) {
            cvars.add("-file");
            int i = 0;
            for (var pwad : config.pwads()) {
                cvars.add("pwad_" + i + ".wad");
                this.iwadData.put("pwad_" + i + ".wad", NucleDoom.WADS.get(pwad));
                i++;
            }
        }
        cvars.addAll(config.cvars());
        this.cvar = new CVarManager(cvars);
        this.configManager = new ConfigManager();
        this.doom = new DoomMain<>();

        var menu = ((Menu) this.doom.menu);
        menu.SaveGame = choice -> {
            /*if (!this.input.sneak()) {
                menu.saveStringEnter = true;
                menu.saveSlot = choice;
                C2JUtils.strcpy(menu.saveOldString, menu.savegamestrings[choice]);
                if (C2JUtils.strcmp(menu.savegamestrings[choice], "empty slot")) {
                    menu.savegamestrings[choice][0] = 0;
                }

                menu.saveCharIndex = C2JUtils.strlen(menu.savegamestrings[choice]);
            } else {*/
            if (C2JUtils.strlen(menu.savegamestrings[choice]) == 0) {
                C2JUtils.strcpy(menu.savegamestrings[choice], "Save Slot " + choice);
            }
            menu.DoSave(choice);
            //}
        };

        for (var x : menu.SaveMenu) {
            x.routine = menu.SaveGame;
        }

        this.screen = new FastCanvasImage(this.doom.graphicSystem.getScreenWidth(), this.doom.graphicSystem.getScreenHeight());
    }

    @Override
    public void startGameLoop() throws Throwable {
        this.doom.setupLoop();
        ((DoomSystem) this.doom.doomSystem).close();
    }

    @Override
    public void clear() {
        ((DoomSystem) this.doom.doomSystem).close();
        this.close = true;
    }

    public CVarManager getCvarManager() {
        return this.cvar;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public void drawFrame() {
        if (this.close) {
            this.doom.soundDriver.ShutdownSound();
            this.doom.music.ShutdownMusic();
            throw new GameClosed(0);
        }

        if (this.canvas == null) {
            return;
        }

        var image = this.doom.graphicSystem.getScreenImage();
        BufferedImage bufferedImage;
        if (image instanceof BufferedImage buf) {
            bufferedImage = buf;
        } else if (image instanceof VolatileImage volatileImage) {
            bufferedImage = volatileImage.getSnapshot();
        } else {
            bufferedImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            DefaultFonts.UNIFONT.drawText(this.screen, "Unsupported renderer", 16, 16, 16, CanvasColor.RED_HIGH);
        }

        int pixels = this.screen.data().length;
        var screen = this.screen.data();
        var buf = bufferedImage.getData().getDataBuffer();

        //CustomColorMap.clearMap();

        var colorModel = bufferedImage.getColorModel();
        for (int i = 0; i < pixels; i++) {
            screen[i] = CanvasUtils.findClosestRawColor(colorModel.getRGB(buf.getElem(i)));
        }

        this.canvas.drawFrame(this.screen);
    }

    @Override
    public void updateKeyboard(PlayerInput input) {
        var menu = this.doom.paused || this.doom.menuactive;

        if (this.input.forward() != input.forward()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.forward() ? evtype_t.ev_keydown : evtype_t.ev_keyup, menu ? Signals.ScanCode.SC_UP : Signals.ScanCode.SC_W));
        }
        if (this.input.backward() != input.backward()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.backward() ? evtype_t.ev_keydown : evtype_t.ev_keyup, menu ? Signals.ScanCode.SC_DOWN :Signals.ScanCode.SC_S));
        }
        if (this.input.left() != input.left()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.left() ? evtype_t.ev_keydown : evtype_t.ev_keyup, menu ? Signals.ScanCode.SC_LEFT : Signals.ScanCode.SC_A));
        }
        if (this.input.right() != input.right()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.right() ? evtype_t.ev_keydown : evtype_t.ev_keyup, menu ? Signals.ScanCode.SC_RIGHT : Signals.ScanCode.SC_D));
        }
        if (this.input.sneak() != input.sneak()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.sneak() ? evtype_t.ev_keydown : evtype_t.ev_keyup, Signals.ScanCode.SC_LSHIFT));
        }
        if (this.input.sprint() != input.sprint()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.sprint() ? evtype_t.ev_keydown : evtype_t.ev_keyup, Signals.ScanCode.SC_TAB));
        }

        if (this.input.jump() != input.jump()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.jump() ? evtype_t.ev_keydown : evtype_t.ev_keyup, Signals.ScanCode.SC_ENTER));
        }

        this.input = input;
    }

    @Override
    public void updateMouse(float v, boolean mouseLeft) {
        double d = 0.6000000238418579 + 0.20000000298023224;
        double e = d * d * d;
        double f = e * 8.0;

        this.mouseEvent.x = (int) (v * 6 / 0.15 / f) ;
        if (mouseLeft) {
            this.mouseEvent.buttons |= event_t.MOUSE_LEFT;
        } else {
            this.mouseEvent.buttons &= 0b110;
        }
        this.doom.PostEvent(this.mouseEvent);
    }

    @Override
    public void selectSlot(int selectedSlot) {
        //if (selectedSlot == 7) {
        //    this.doom.graphicSystem.setUsegamma(this.doom.graphicSystem.getUsegamma() + 1);
        //    return;
        //}

        var sig = Signals.ScanCode.values()[Signals.ScanCode.SC_1.ordinal() + selectedSlot];
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, sig));
        this.pressNum[selectedSlot] = 2;
    }

    @Override
    public void pressE() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_E));
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_Y));

        this.pressE = 5;
    }

    @Override
    public void pressQ() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_N));
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_BACKSPACE));
        this.pressQ = 5;
    }

    @Override
    public void pressF() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_ESCAPE));
        this.pressF = 5;
    }

    @Override
    public void tick() {
        if (--pressF == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_ESCAPE));
        }

        if (--pressE == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_E));
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_Y));
        }

        if (--pressQ == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_N ));
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_BACKSPACE));
        }

        for (int i = 0; i < 9; i++) {
            if (--this.pressNum[i] == 0) {
                var sig = Signals.ScanCode.values()[Signals.ScanCode.SC_1.ordinal() + i];
                this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, sig));
            }
        }

        if (NucleDoom.IS_DEV) {
            //SoundMap.updateSoundMap();
        }
    }

    @Override
    public void extractAudio(BiConsumer<String, byte[]> consumer) {
        new MinecraftSoundDriver(this.doom, this) {
            void callInit() {
                initSound16();
            };
        }.callInit();

        for (var sound : sounds.S_sfx) {
            consumer.accept(sound.name, sound.data);
        }
    }

    public void playSound(SoundTarget target, SoundEvent soundVanilla, float pitch, float volume) {
        if (this.canvas != null) {
            this.canvas.playSound(target, soundVanilla, pitch, volume);
        }
    }

    public boolean supportsSoundTarget(SoundTarget target) {
        return this.canvas != null && this.canvas.supportsSoundTargets(target);
    }


    @Nullable
    public Supplier<InputStream> getResourceStream(String path) {
        if (path.startsWith("./")) {
            path = path.substring("./".length());
        }

        if (path.equals(this.wadName)) {
            return () -> new ByteArrayInputStream(this.wadData);
        }

        var iwad = this.iwadData.get(path);
        if (iwad != null) {
            return () -> new ByteArrayInputStream(iwad);
        }

        if (this.resourceCache.containsKey(path)) {
            return this.resourceCache.get(path);
        }

        var remap = this.config.resourceMap().get(path);
        if (remap != null) {
            var optional = this.resource.getResource(remap);
            if (optional.isPresent()) {
                var resource = optional.get();
                Supplier<InputStream> supplier = () -> {
                    try {
                        return resource.getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };
                this.resourceCache.put(path, supplier);
                return supplier;
            }
        }

        if (path.equals("mochadoom.cfg")) {
            return () -> {
                try {
                    return Files.newInputStream(FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID).get()
                            .findPath((FabricLoader.getInstance().isDevelopmentEnvironment() ? "" : "/") + "data/nucledoom/mochadoom.cfg").get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        } else if (path.equals("default.cfg")) {
            return () -> {
                try {
                    return Files.newInputStream(FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID).get()
                            .findPath((FabricLoader.getInstance().isDevelopmentEnvironment() ? "" : "/") + "data/nucledoom/default.cfg").get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }


        var pth = FabricLoader.getInstance().getGameDir().resolve(path);

        if (Files.exists(pth)) {
            return () -> {
                try {
                    return Files.newInputStream(pth);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        return null;
    }

    public void mainLoopStart() {
        this.doom.PostEvent(this.mouseEvent);
    }
}
