package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.core.*;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.nucledoom.ExtraFonts;
import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.PlayerSaveData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.FilledMapItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GameCanvas {
    private static final Logger LOGGER = LoggerFactory.getLogger("GameCanvas");

    private static final CanvasImage DEFAULT_BACKGROUND = readImage("default_background");
    private static final CanvasImage DEFAULT_OVERLAY = readImage("default_overlay");
    private static final CanvasImage DEFAULT_OVERLAY_RESET = readImage("default_overlay_reset");
    private static final int BACKGROUND_SCALE = 1;
    private final MinecraftServer server;
    private final String title;

    private Throwable error;

    private float mouseX = 0;
    private float mouseY = 0;
    private boolean mouseLeft;
    private float previousMouseX;
    private float previousMouseY;

    private static CanvasImage readImage(String path) {
        CanvasImage temp;
        try {
            temp = CanvasImage.from(ImageIO.read(
                    Files.newInputStream(FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID).get().findPath("data/nucledoom/background/" + path + ".png").get())));
        } catch (Throwable e) {
            temp = new CanvasImage(128, 128);

            e.printStackTrace();
        }
        return temp;
    }

    private static final int RENDER_SCALE = 1;
    private static final int MAP_SIZE = FilledMapItem.field_30907;

    private static  final int DEFAULT_SCREEN_WIDTH = 320;
    private static final int DEFAULT_SCREEN_HEIGHT = 200;

    private final int scale;

    private final int screenWidth;
    private final int screenHeight;
    private final int sectionHeight;
    private final int sectionWidth;

    private final int drawOffsetX;
    private final int drawOffsetY;

    private final DoomConfig config;

    private final CombinedPlayerCanvas canvas;

    private PlayerInterface playerInterface = PlayerInterface.NO_OP;

    private long previousFrameTime = -1;
    private DoomGame game = null;
    private JarGameClassLoader classLoader = null;

    public GameCanvas(DoomConfig config, String title, MinecraftServer server) {
        this.config = config;
        this.server = server;
        this.title = title;

        this.scale = 1;
        this.screenHeight = DEFAULT_SCREEN_HEIGHT * scale;
        this.screenWidth = DEFAULT_SCREEN_WIDTH * scale;
        this.sectionHeight = 5 * scale;
        this.sectionWidth = 8 * scale;
        this.drawOffsetX = sectionWidth * 64 - screenWidth / 2;
        this.drawOffsetY = sectionHeight * 64 - screenHeight / 2;

        this.canvas = DrawableCanvas.create(sectionWidth, sectionHeight);
        CanvasUtils.clear(this.canvas, CanvasColor.GRAY_HIGH);
        if (DEFAULT_BACKGROUND != null) {
            var background = DEFAULT_BACKGROUND;
            var width = background.getWidth() * BACKGROUND_SCALE * scale;
            var height = background.getHeight() * BACKGROUND_SCALE * scale;
            var repeatsX = Math.ceilDiv(this.canvas.getWidth(), width);
            var repeatsY = Math.ceilDiv(this.canvas.getHeight(), height);

            for (int x = 0; x < repeatsX; x++) {
                for (int y = 0; y < repeatsY; y++) {
                    CanvasUtils.draw(this.canvas, x * width, y * height, width, height, background);
                }
            }
        }

        if (DEFAULT_OVERLAY != null) {
            var background = DEFAULT_OVERLAY;
            var width = background.getWidth() * BACKGROUND_SCALE * scale;
            var height = background.getHeight() * BACKGROUND_SCALE * scale;
            CanvasUtils.draw(this.canvas, this.canvas.getWidth() / 2 - width / 2, this.canvas.getHeight() / 2 - height / 2, width, height, background);
        }

        CanvasUtils.fill(this.canvas, drawOffsetX, drawOffsetY, drawOffsetX + screenWidth, drawOffsetY + screenHeight, CanvasColor.BLACK_NORMAL);

        DefaultFonts.UNIFONT.drawText(this.canvas, this.title, drawOffsetX + 2, drawOffsetY - 16 - 4, 16, CanvasColor.WHITE_HIGH);


        var text = """
                  ## Controls
                  - Move with WSAD
                  - Run with SHIFT
                  - Mouse to look around
                  - Left click to shoot
                  - Right click to use
                  - 1-7 to select weapon
                  - F for pause/menu
                  - SPACE to select
                  - E to accept
                  - Q to go back
                """;

        ExtraFonts.OPEN_ZOO_4x8.drawText(this.canvas, text, drawOffsetX - 91 * scale, drawOffsetY + 70 * scale, 8 * scale, CanvasColor.BLACK_HIGH);
    }

    public void setPlayerInterface(PlayerInterface playerInterface) {
        this.playerInterface = playerInterface;
    }

    private void drawError(Throwable e) {
        var width = DefaultFonts.UNIFONT.getTextWidth("ERROR!", 16);

        CanvasUtils.fill(this.canvas, (screenWidth - width) / 2 - 5 + drawOffsetX, 11 + drawOffsetY,
                (screenWidth - width) / 2 + width + 5 + drawOffsetX, 16 * 2 + 5 + drawOffsetY, CanvasColor.BLUE_HIGH);
        //CanvasUtils.fill(this.canvas, 0, 0, SCREEN_HEIGHT, SCREEN_WIDTH, CanvasColor.BLUE_HIGH);
        DefaultFonts.UNIFONT.drawText(this.canvas, "ERROR!", (screenWidth - width) / 2 + 1 + drawOffsetX, 17 + drawOffsetY, 16, CanvasColor.BLACK_LOW);
        DefaultFonts.UNIFONT.drawText(this.canvas, "ERROR!", (screenWidth - width) / 2 + drawOffsetX, 16 + drawOffsetY, 16, CanvasColor.RED_HIGH);

        String message1;
        String message2;

        message1 = "Runtime error!";
        message2 = e.toString();

        if (e instanceof GameClosed gameClosed) {
            message1 = "Game closed with status " + gameClosed.status();
        }


        List<String> message2Split = new ArrayList<>();

        var builder = new StringBuilder();

        for (var x : message2.toCharArray()) {
            if (x == '\n' || DefaultFonts.VANILLA.getTextWidth(builder.toString() + x, 8) > screenWidth - 10) {
                message2Split.add(builder.toString());
                builder = new StringBuilder();
            }
            if (x != '\n') {
                builder.append(x);
            }
        }
        message2Split.add(builder.toString());

        CanvasUtils.fill(this.canvas, 0 + drawOffsetX, 63 + drawOffsetY,
                screenWidth + drawOffsetX, 65 + 8 + drawOffsetY, CanvasColor.BLUE_HIGH);

        DefaultFonts.VANILLA.drawText(this.canvas, message1, 5 + drawOffsetX, 64 + drawOffsetY, 8, CanvasColor.WHITE_HIGH);

        CanvasUtils.fill(this.canvas, 0 + drawOffsetX, 63 + 10 + drawOffsetY,
                screenWidth + drawOffsetX, 65 + 10 + message2Split.size() * 10 + drawOffsetY, CanvasColor.BLUE_HIGH);
        for (int i = 0; i < message2Split.size(); i++) {
            DefaultFonts.VANILLA.drawText(this.canvas, message2Split.get(i), 5 + drawOffsetX, 64 + 10 + 10 * i + drawOffsetY, 8, CanvasColor.WHITE_HIGH);
        }

        this.canvas.sendUpdates();
    }

    public void start() {
        synchronized (this) {
            try {
                var open = DoomGame.create(this, this.playerInterface.getSaveData(), this.config, this.server.getResourceManager(), this.scale);
                this.game = open.game();
                this.classLoader = open.loader();
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


    public BlockPos getDisplayPos() {
        return new BlockPos(-sectionWidth, sectionHeight + 100, 0);
    }

    public Vec3d getSpawnPos() {
        BlockPos displayPos = this.getDisplayPos();

        return new Vec3d(displayPos.getX() + sectionWidth * 0.5, displayPos.getY() - sectionHeight * 0.5f + 1, 1.5 * scale + 0.01f);
    }

    public int getSpawnAngle() {
        return 180;
    }

    public PlayerCanvas getCanvas() {
        return this.canvas;
    }


    public void updateKeyboard(PlayerInput input) {
        if (this.game != null) {
            this.game.updateKeyboard(input);
        }
    }

    public void updateMousePosition(float x, float y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void pressMouseRight(boolean b) {
        if (b) {
            this.pressE();
        }
    }

    public void selectSlot(int selectedSlot) {
        synchronized (this) {
            if (this.game != null) {
                this.game.selectSlot(selectedSlot);
            }
        }
    }

    public void drawFrame(DrawableCanvas canvas) {
        var frame = System.currentTimeMillis();

        if (DEFAULT_OVERLAY_RESET != null) {
            var background = DEFAULT_OVERLAY_RESET;
            var width = background.getWidth() * BACKGROUND_SCALE * scale;
            var height = background.getHeight() * BACKGROUND_SCALE * scale;

            CanvasUtils.draw(this.canvas, this.canvas.getWidth() / 2 - width / 2, this.canvas.getHeight() / 2 - height / 2, width, height,
                    background);
        }
        var text = String.format("%s - %s MS", this.title, frame - previousFrameTime);
        DefaultFonts.UNIFONT.drawText(this.canvas, text, drawOffsetX + 2 * scale, drawOffsetY - (16 + 4) * scale, 16 * scale, CanvasColor.WHITE_HIGH);

        CanvasUtils.draw(this.canvas, drawOffsetX, drawOffsetY, canvas);

        previousFrameTime = frame;
        this.canvas.sendUpdates();
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
        this.mouseLeft = down;
    }

    public void playSound(SoundTarget target, SoundEvent soundEvent, float pitch, float volume) {
        if (target.isSupported(false, false)) {
            this.playerInterface.playSound(soundEvent, pitch, volume);
        }
    }

    public DoomConfig getConfig() {
        return this.config;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public boolean supportsSoundTargets(SoundTarget target) {
        return target.isSupported(false, false);
    }

    public void clientTick() {
        synchronized (this) {
            if (this.game != null) {
                this.game.updateMouse(MathHelper.subtractAngles(this.previousMouseX, this.mouseX), this.mouseLeft);
            }
        }
        this.previousMouseX = this.mouseX;
        this.previousMouseY = this.mouseY;
        this.mouseX = 0;
        this.mouseY = 0;
    }


    public interface PlayerInterface {
        PlayerInterface NO_OP = new PlayerInterface() {
            @Override
            public void playSound(SoundEvent soundEvent, float pitch, float volume) {

            }

            @Override
            public void close() {

            }

            @Override
            public PlayerSaveData getSaveData() {
                return null;
            }
        };

        void playSound(SoundEvent soundEvent, float pitch, float volume);

        void close();

        @Nullable
        PlayerSaveData getSaveData();
    }
}
