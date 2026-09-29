package eu.pb4.nucledoom.game;

import eu.pb4.nucledoom.PlayerSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GameHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("GameHandler");
    private final MinecraftServer server;
    private final String title;

    private Throwable error;

    private float mouseX = 0;
    private float mouseY = 0;
    private boolean mouseLeft;
    private float previousMouseX;
    private float previousMouseY;
    private final DoomConfig config;

    private GameCanvas canvas;

    private PlayerInterface playerInterface = PlayerInterface.NO_OP;
    private DoomGame game = null;
    private JarGameClassLoader classLoader = null;
    private int scale = 1;

    public GameHandler(DoomConfig config, String title, MinecraftServer server) {
        this.config = config;
        this.server = server;
        this.title = title;
    }

    public void updateCanvas(boolean trueRgb) {
        if (this.canvas != null) {
            this.canvas.close();
        }
        this.canvas = new GameCanvas(title, trueRgb, this.game != null ? this.game.getScreenInfo() : DoomGame.ScreenInfo.FALLBACK, this.game != null ? this.game.getControls() : "");
    }

    public void setPlayerInterface(PlayerInterface playerInterface) {
        this.playerInterface = playerInterface;
    }

    public void drawError(Throwable e) {
        this.getOrInitCanvas().drawError(e);
    }

    private GameCanvas getOrInitCanvas() {
        if (this.canvas == null) {
            this.updateCanvas(false);
        }
        return this.canvas;
    }

    public void start(DoomGame.GameOpener opener) {
        synchronized (this) {
            try {
                var open = opener.createGame(this, this.playerInterface.getSaveData(), this.config, this.server.getResourceManager());
                this.game = open.game();
                this.classLoader = open.loader();
                var screenInfo = this.game.getScreenInfo();
                if (this.canvas == null || screenInfo.width() != this.canvas.getScreenWidth() || screenInfo.height() != this.canvas.getScreenHeight()) {
                    this.updateCanvas(false);
                    this.playerInterface.reconfigureCanvas();
                } else {
                    this.canvas.updateBackgroundTexture(screenInfo.background(), screenInfo.overlay(), screenInfo.overlayReset(), screenInfo.backgroundScale());
                    this.canvas.setControls(game.getControls());
                    this.canvas.drawBackground();
                }
            } catch (Throwable e) {
                this.error = e;
                this.drawError(e);
                e.printStackTrace();
                return;
            }
        }

        try {
            this.game.startGameLoop();
        } catch (Throwable e) {
            if (e instanceof GameClosed gameClosed && gameClosed.status() == 0) {
                this.game.clear();
                this.server.execute(() -> {
                    this.playerInterface.close();
                });
                return;
            }

            this.error = e;
            this.drawError(e);
            e.printStackTrace();
        }
        this.game.clear();
    }

    public Vec3 getSpawnPos() {
        return this.canvas != null ? this.canvas.getSpawnPos() : Vec3.ZERO ;
    }

    public int getSpawnAngle() {
        return 180;
    }

    public GameCanvas getCanvas() {
        return this.canvas;
    }


    public void updateKeyboard(Input input) {
        if (this.game != null) {
            this.game.updateKeyboard(input);
        }
    }

    public void updateMousePosition(float x, float y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void pressMouseRight(boolean value) {
        synchronized (this) {
            if (this.game != null) {
                this.game.pressMouseRight(value);
            }
        }
    }

    public void selectSlot(int selectedSlot) {
        synchronized (this) {
            if (this.game != null) {
                this.game.selectSlot(selectedSlot);
            }
        }
    }

    public void pressE() {
        synchronized (this) {
            if (this.game != null) {
                this.game.pressE();
            }
        }
    }

    public void pressQ() {
        synchronized (this) {
            if (this.game != null) {
                this.game.pressQ();
            }
        }
    }

    public void pressF() {
        synchronized (this) {
            if (this.game != null) {
                this.game.pressF();
            }
        }
    }

    public void destroy() {
        if (this.game != null) {
            try {
                this.game.clear();
            } catch (Throwable e) {
                // Ignore
            }
        }
        if (this.classLoader != null) {
            try {
                this.classLoader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void tick() {
        if (this.game != null) {
            this.game.tick();
        }
    }

    public void pressMouseLeft(boolean down) {
        synchronized (this) {
            if (this.game != null) {
                this.game.pressMouseLeft(down);
            }
        }
    }

    public void playSound(SoundTarget target, SoundEvent soundEvent, float pitch, float volume, long seed) {
        if (supportsSoundTargets(target)) {
            this.playerInterface.playSound(soundEvent, pitch, volume, seed);
        }
    }

    public DoomConfig getConfig() {
        return this.config;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public boolean supportsSoundTargets(SoundTarget target) {
        var hasResourcePack = this.playerInterface.hasResourcePack();
        return target.isSupported(false, false);
    }

    public void clientTick() {
        synchronized (this) {
            if (this.game != null) {
                this.game.updateMouse(Mth.degreesDifference(this.previousMouseX, this.mouseX), Mth.degreesDifference(this.previousMouseY, this.mouseY));
            }
        }
        this.previousMouseX = this.mouseX;
        this.previousMouseY = this.mouseY;
        this.mouseX = 0;
        this.mouseY = 0;
    }

    public boolean onChat(String s) {
        synchronized (this) {
            if (this.game != null) {
                return this.game.onChat(s);
            }
        }
        return false;
    }

    public int getScale() {
        return this.scale;
    }

    public boolean hasOpPerms() {
        return this.playerInterface.hasOpPerms();
    }

    public PlayerInterface playerInterface() {
        return playerInterface;
    }

    public interface PlayerInterface {
        PlayerInterface NO_OP = new PlayerInterface() {
            @Override
            public void playSound(SoundEvent soundEvent, float pitch, float volume, long seed) {

            }

            @Override
            public void close() {

            }

            @Override
            public PlayerSaveData getSaveData() {
                return null;
            }

            @Override
            public void reconfigureCanvas() {

            }

            @Override
            public boolean hasOpPerms() {
                return false;
            }

            @Override
            public boolean hasResourcePack() {
                return false;
            }
        };

        void playSound(SoundEvent soundEvent, float pitch, float volume, long seed);

        void close();

        @Nullable
        PlayerSaveData getSaveData();

        boolean hasOpPerms();

        boolean hasResourcePack();

        void reconfigureCanvas();
    }
}
