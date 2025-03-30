package eu.pb4.nucledoom.game.doom;

import doom.*;
import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.game.DoomConfig;
import eu.pb4.nucledoom.game.GameCanvas;
import g.Signals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.PlayerInput;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DoomGame {
    public static final ThreadLocal<DoomGame> GAME = new ThreadLocal<>();

    private final DoomMain<?, ?> doom;
    private final CVarManager cvar;
    private final DoomConfig config;
    private final GameCanvas canvas;
    private final ConfigManager configManager;

    private volatile boolean close = false;
    private PlayerInput input = PlayerInput.DEFAULT;
    private int pressF;
    private int pressE;
    private int pressQ;

    private final int[] pressNum = new int[9];
    private final event_t.mouseevent_t mouseEvent = new event_t.mouseevent_t(evtype_t.ev_mouse, 0, 0, 0);
    private boolean resentMouse;


    public DoomGame(GameCanvas gameCanvas, ResourceManager resourceManager, int scale) throws IOException {
        this.canvas = gameCanvas;
        this.config = this.canvas.getConfig();
        GAME.set(this);
        var cvars = new ArrayList<String>();
        cvars.addAll(List.of("-multiply", String.valueOf(scale), "-novolatileimage"));
        cvars.addAll(config.cvars());
        this.cvar = new CVarManager(cvars);
        this.configManager = new ConfigManager();
        this.doom = new DoomMain<>();
    }

    public void startGameLoop() {
        try {
            this.doom.setupLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        GAME.remove();
        this.doom.soundDriver.ShutdownSound();
        this.doom.music.ShutdownMusic();
        this.close = true;
        this.doom.wadLoader.CloseAllHandles();
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
            throw new RuntimeException("Closed!");
        }

        var image = this.doom.graphicSystem.getScreenImage();
        BufferedImage bufferedImage;
        if (image instanceof BufferedImage buf) {
            bufferedImage = buf;
        } else if (image instanceof VolatileImage volatileImage) {
            bufferedImage = volatileImage.getSnapshot();
        } else {
            bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        this.canvas.drawFrame(bufferedImage);
        if (this.resentMouse) {
            this.doom.PostEvent(this.mouseEvent);
            this.resentMouse = false;
        } else {
            this.mouseEvent.x = 0;
        }
    }

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
            this.doom.PostEvent(new event_t.keyevent_t(input.sprint() ? evtype_t.ev_keydown : evtype_t.ev_keyup, Signals.ScanCode.SC_LCTRL));
        }

        if (this.input.jump() != input.jump()) {
            this.doom.PostEvent(new event_t.keyevent_t(input.jump() ? evtype_t.ev_keydown : evtype_t.ev_keyup, Signals.ScanCode.SC_ENTER));
        }

        this.input = input;
    }

    public void moveMouse(float v) {
        double d = 0.6000000238418579 + 0.20000000298023224;
        double e = d * d * d;
        double f = e * 8.0;

        this.mouseEvent.x = (int) (v * 4 / 0.15 / f) ;
        this.doom.PostEvent(this.mouseEvent);
        this.resentMouse = true;
    }

    public void pressMouseLeft(boolean down) {
        if (down) {
            this.mouseEvent.buttons |= event_t.MOUSE_LEFT;
        } else {
            this.mouseEvent.buttons ^= event_t.MOUSE_LEFT;
        }
        this.doom.PostEvent(this.mouseEvent);
    }

    public void selectSlot(int selectedSlot) {
        var sig = Signals.ScanCode.values()[Signals.ScanCode.SC_1.ordinal() + selectedSlot];
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, sig));
        this.pressNum[selectedSlot] = 2;
    }

    public void pressE() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_E));
        this.pressE = 5;
    }

    public void pressQ() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_BACKSPACE));
        this.pressQ = 5;
    }

    public void pressF() {
        this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keydown, Signals.ScanCode.SC_ESCAPE));
        this.pressF = 5;
    }

    public void tick() {
        if (--pressF == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_ESCAPE));
        }

        if (--pressE == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_E));
        }

        if (--pressQ == 0) {
            this.doom.PostEvent(new event_t.keyevent_t(evtype_t.ev_keyup, Signals.ScanCode.SC_BACKSPACE ));
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

    public void playSound(SoundEvent soundEvent, float pitch, float volume) {
        this.canvas.playSound(soundEvent, pitch, volume);
    }

    @Nullable
    public Supplier<InputStream> getResourceStream(String path) {
        if (path.startsWith("./")) {
            path = path.substring("./".length());
        }

        var remap = this.config.resourceMap().get(path);
        if (remap != null) {
            var resource = this.canvas.getServer().getResourceManager().getResource(remap);
            if (resource.isPresent()) {
                return () -> {
                    try {
                        return resource.get().getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        }

        if (path.equals("mochadoom.cfg")) {
            return () -> {
                try {
                    return Files.newInputStream(FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID).get().findPath("data/nucledoom/default_config.cfg").get());
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
}
