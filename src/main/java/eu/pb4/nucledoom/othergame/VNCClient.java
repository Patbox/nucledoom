package eu.pb4.nucledoom.othergame;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.mapcanvas.impl.image.RawImage;
import eu.pb4.nucledoom.PlayerSaveData;
import eu.pb4.nucledoom.game.*;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Input;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class VNCClient implements DoomGame {
    private final GameHandler handler;
    private ScreenInfo screenInfo;
    private volatile boolean close = false;
    private Input input = Input.EMPTY;
    private CanvasImage screen = new CanvasImage(128, 128);
    private Int2IntMap keys = new Int2IntArrayMap();
    private VernacularClient client;
    private int mouseX;
    private int mouseY;

    public VNCClient(@Nullable GameHandler gameHandler,
                     @Nullable PlayerSaveData saveData,
                     DoomConfig config,
                     ResourceManager resourceManager) throws Throwable {
        this.screenInfo = ScreenInfo.FALLBACK;
        this.handler = gameHandler;
    }

    @Override
    public ScreenInfo getScreenInfo() {
        return this.screenInfo;
    }

    @Override
    public boolean onChat(String message) {
        this.client.type(message);
        return true;
    }

    @Override
    public void startGameLoop() throws Throwable {
        try {
            var config = new VernacularConfig();
            this.client = new VernacularClient(config);

            // Select 8-bits per pixel indexed color, or 8/16/24 bits per pixel true color
            config.setColorDepth(ColorDepth.BPP_24_TRUE);
            config.setTargetFramesPerSecond(60);

            // Set up callbacks for the various events that can happen in a VNC session

            // Exception handler
            config.setErrorListener(Throwable::printStackTrace);

            // Password supplier - this is only invoked if the remote server requires authentication
            config.setPasswordSupplier(() -> "");

            // Handle system bell events from the remote host
            config.setBellListener(v -> System.out.println("DING!"));

            // Receive screen updates from the remote host
            // The 'image' parameter is a java.awt.Image containing a current snapshot of the remote desktop
            // Expect this event to be triggered several times per second
            config.setScreenUpdateListener(img -> {
                var screen = CanvasImage.from((BufferedImage) img);

                if (this.screen == null || this.screen.getHeight() != screen.getHeight() || this.screen.getWidth() != screen.getWidth()) {
                    this.screenInfo = new ScreenInfo(screen.getWidth(), screen.getHeight(), screenInfo.background(), screenInfo.overlay(), screenInfo.overlayReset(), screenInfo.backgroundScale());
                    this.handler.updateCanvas(false);
                    this.handler.playerInterface().reconfigureCanvas();
                    this.mouseX = this.screenInfo.width() / 2;
                    this.mouseY = this.screenInfo.height() / 2;
                }
                this.screen = screen;
            });

            // Start the VNC session
            client.start("pblaptop.local", 5900);

            while (true) {
                this.drawFrame();
                if (this.close) break;
                Thread.sleep(10);
            }
        } finally {

        }
    }

    @Override
    public void clear() {
        this.close = true;
        if (this.client != null) {
            this.client.stop();
        }
    }

    public void drawFrame() {
        if (this.close) {
            throw new GameClosed(0);
        }

        this.handler.getCanvas().drawFrame(this.screen);
    }

    @Override
    public void updateKeyboard(Input input) {
        if (this.input.right() != input.right()) pressEvent(KeyEvent.VK_D, input.right());
        if (this.input.left() != input.left()) pressEvent(KeyEvent.VK_A, input.left());
        if (this.input.forward() != input.forward()) pressEvent(KeyEvent.VK_W, input.forward());
        if (this.input.backward() != input.backward()) pressEvent(KeyEvent.VK_S, input.backward());
        if (this.input.shift() != input.shift()) pressEvent(KeyEvent.VK_SHIFT, input.shift());
        if (this.input.sprint() != input.sprint()) pressEvent(KeyEvent.CTRL_DOWN_MASK, input.sprint());
        if (this.input.jump() != input.jump()) pressEvent(KeyEvent.VK_SPACE, input.jump());

        this.input = input;
    }

    @Override
    public void updateMouse(float xDelta, float yDelta) {
        double d = 0.6000000238418579 + 0.20000000298023224;
        double e = d * d * d;
        double f = e * 8.0;


        var newX = this.mouseX + (int) (xDelta * 6 / 0.15 / f / 1.5);
        var newY = this.mouseY + (int) (yDelta * 6 / 0.15 / f / 1.5);
        if (this.mouseX != newX || this.mouseY != newY) {
            this.client.moveMouse(newX, newY);
            this.mouseX = newX;
            this.mouseY = newY;
        }
    }

    @Override
    public void pressMouseLeft(boolean value) {
        this.client.updateMouseButton(1, value);
    }

    @Override
    public void pressMouseRight(boolean value) {
        this.client.updateMouseButton(2, value);
    }

    private void pressEvent(int key, boolean val) {
        this.client.updateKey(key, val);
    }

    private void pressTimed(int key) {
        if (this.keys.getOrDefault(key, -1) == -1) {
            this.client.updateKey(key, true);
        }
        this.keys.put(key, 2);
    }


    @Override
    public void selectSlot(int selectedSlot) {
        this.pressTimed(KeyEvent.VK_1 + selectedSlot);
    }

    @Override
    public void pressE() {
        this.pressTimed(KeyEvent.VK_ESCAPE);
    }

    @Override
    public void pressQ() {
        this.pressTimed(KeyEvent.VK_Q);
    }

    @Override
    public void pressF() {
        this.pressTimed(KeyEvent.VK_F);
    }

    @Override
    public void tick() {
        var copy = new IntArrayList(this.keys.keySet());
        for (int i = 0; i < copy.size(); i++) {
            var key = copy.getInt(i);
            var val = this.keys.get(key) - 1;
            if (val == 0) {
                this.keys.remove(key);
                this.client.updateKey(key, false);
            } else {
                this.keys.put(key, val);
            }
        }
    }

    @Override
    public void extractAudio(BiConsumer<String, byte[]> consumer) {}

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
        return "";
    }
}
