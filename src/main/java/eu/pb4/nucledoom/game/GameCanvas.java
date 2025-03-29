package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.core.*;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.nucledoom.DoomBox;
import eu.pb4.nucledoom.game.audio.AudioController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.FilledMapItem;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GameCanvas {
    private static final Logger LOGGER = LoggerFactory.getLogger("GameCanvas");

    private static final CanvasImage DEFAULT_BACKGROUND = readImage("default_background");
    private static final CanvasImage DEFAULT_OVERLAY = readImage("default_overlay");
    private static final int BACKGROUND_SCALE = 2;

    private Throwable error;
    private float mouseX;
    private float mouseY;

    private static CanvasImage readImage(String path) {
        CanvasImage temp;
        try {
            temp = CanvasImage.from(ImageIO.read(
                    Files.newInputStream(FabricLoader.getInstance().getModContainer(DoomBox.MOD_ID).get().findPath("data/nucledoom/background/" + path + ".png").get())));
        } catch (Throwable e) {
            temp = new CanvasImage(128, 128);

            e.printStackTrace();
        }
        return temp;
    }

    private static final int RENDER_SCALE = 1;
    private static final int MAP_SIZE = FilledMapItem.field_30907;

    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 200;
    private static final int SECTION_SIZE = MathHelper.ceil(SCREEN_WIDTH * RENDER_SCALE / (double) MAP_SIZE);
    private static final int SECTION_HEIGHT = 5;
    private static final int SECTION_WIDTH = 8;

    private static final int DRAW_OFFSET_X = (SECTION_WIDTH * 64 - SCREEN_WIDTH / 2);
    private static final int DRAW_OFFSET_Y = (SECTION_HEIGHT * 64 - SCREEN_HEIGHT / 2);

    private final DoomConfig config;

    private final CombinedPlayerCanvas canvas;

    private final AudioController audioController;

    private long previousFrameTime = -1;
    private DoomGame game = null;

    public GameCanvas(DoomConfig config, AudioController audioController) {
        this.config = config;
        this.audioController = audioController;

        this.canvas = DrawableCanvas.create(SECTION_WIDTH, SECTION_HEIGHT);
        CanvasUtils.clear(this.canvas, CanvasColor.GRAY_HIGH);
        if (DEFAULT_BACKGROUND != null) {
            var background = DEFAULT_BACKGROUND;
            var width = background.getWidth() * BACKGROUND_SCALE;
            var height = background.getHeight() * BACKGROUND_SCALE;
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
            var width = background.getWidth() * BACKGROUND_SCALE;
            var height = background.getHeight() * BACKGROUND_SCALE;
            CanvasUtils.draw(this.canvas, this.canvas.getWidth() / 2 - width / 2, this.canvas.getHeight() / 2 - height / 2, width, height, background);
        }

        var text = """
                        ↑ | [W]
                        → | [D]
                        ← | [A]
                        ↓ | [S]
                        """;


            text += """
                    X | [Space]
                    Z | [Shift]
                    """;


        DefaultFonts.VANILLA.drawText(this.canvas, text, DRAW_OFFSET_X - 78, DRAW_OFFSET_Y + SCREEN_HEIGHT - 59, 8, CanvasColor.BLACK_HIGH);
        DefaultFonts.VANILLA.drawText(this.canvas, text, DRAW_OFFSET_X - 79, DRAW_OFFSET_Y + SCREEN_HEIGHT - 60, 8, CanvasColor.WHITE_HIGH);
    }
    private void drawError(Throwable e) {
        var width = DefaultFonts.VANILLA.getTextWidth("ERROR!", 16);

        CanvasUtils.fill(this.canvas, (SCREEN_WIDTH - width) / 2 - 5 + DRAW_OFFSET_X, 11 + DRAW_OFFSET_Y,
                (SCREEN_WIDTH - width) / 2 + width + 5 + DRAW_OFFSET_X, 16 * 2 + 5 + DRAW_OFFSET_Y, CanvasColor.BLUE_HIGH);
        //CanvasUtils.fill(this.canvas, 0, 0, SCREEN_HEIGHT, SCREEN_WIDTH, CanvasColor.BLUE_HIGH);
        DefaultFonts.VANILLA.drawText(this.canvas, "ERROR!", (SCREEN_WIDTH - width) / 2 + 1 + DRAW_OFFSET_X, 17 + DRAW_OFFSET_Y, 16, CanvasColor.BLACK_LOW);
        DefaultFonts.VANILLA.drawText(this.canvas, "ERROR!", (SCREEN_WIDTH - width) / 2 + DRAW_OFFSET_X, 16 + DRAW_OFFSET_Y, 16, CanvasColor.RED_HIGH);

        String message1;
        String message2;


        message1 = "Runtime error!";
        message2 = e.getMessage();


        List<String> message2Split = new ArrayList<>();

        var builder = new StringBuilder();

        for (var x : message2.toCharArray()) {
            if (x == '\n' || DefaultFonts.VANILLA.getTextWidth(builder.toString() + x, 8) > SCREEN_WIDTH - 10) {
                message2Split.add(builder.toString());
                builder = new StringBuilder();
            }
            if (x != '\n') {
                builder.append(x);
            }
        }
        message2Split.add(builder.toString());

        CanvasUtils.fill(this.canvas, 0 + DRAW_OFFSET_X, 63 + DRAW_OFFSET_Y,
                SCREEN_WIDTH + DRAW_OFFSET_X, 65 + 8 + DRAW_OFFSET_Y, CanvasColor.BLUE_HIGH);

        DefaultFonts.VANILLA.drawText(this.canvas, message1, 5 + DRAW_OFFSET_X, 64 + DRAW_OFFSET_Y, 8, CanvasColor.WHITE_HIGH);

        CanvasUtils.fill(this.canvas, 0 + DRAW_OFFSET_X, 63 + 10 + DRAW_OFFSET_Y,
                SCREEN_WIDTH + DRAW_OFFSET_X, 65 + 10 + message2Split.size() * 10 + DRAW_OFFSET_Y, CanvasColor.BLUE_HIGH);
        for (int i = 0; i < message2Split.size(); i++) {
            DefaultFonts.VANILLA.drawText(this.canvas, message2Split.get(i), 5 + DRAW_OFFSET_X, 64 + 10 + 10 * i + DRAW_OFFSET_Y, 8, CanvasColor.WHITE_HIGH);
        }
    }

    public void start() {
        synchronized (this) {
            try {
                this.game = new DoomGame(this);
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
            this.error = e;
            this.drawError(e);
            e.printStackTrace();
        }
        this.game.clear();
    }


    public BlockPos getDisplayPos() {
        return new BlockPos(-SECTION_WIDTH, SECTION_HEIGHT + 100, 0);
    }

    public Vec3d getSpawnPos() {
        BlockPos displayPos = this.getDisplayPos();

        return new Vec3d(displayPos.getX() + SECTION_WIDTH * 0.5, displayPos.getY() - SECTION_HEIGHT * 0.5f + 1, 1.5);
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
        synchronized (this) {
            this.game.moveMouse(MathHelper.subtractAngles(this.mouseX, x));
        }
        this.mouseX = x;
        this.mouseY = y;
    }

    public void pressMouseRight() {

    }

    public void selectSlot(int selectedSlot) {
        synchronized (this) {
            this.game.selectSlot(selectedSlot);
        }
    }

    public void drawFrame(BufferedImage screenImage) {
        var canvasImage = CanvasImage.from(screenImage);
        var frame = System.currentTimeMillis();
        DefaultFonts.VANILLA.drawText(canvasImage, (1000 / (frame - previousFrameTime)) + " FPS", 6, 6, 8, CanvasColor.WHITE_LOW);
        CanvasUtils.draw(this.canvas, DRAW_OFFSET_X, DRAW_OFFSET_Y, canvasImage);
        previousFrameTime = frame;
        this.canvas.sendUpdates();
    }

    public void pressE() {
        synchronized (this) {
            this.game.pressE();
        }
    }

    public void pressQ() {
        synchronized (this) {
            this.game.pressQ();
        }
    }

    public void pressF() {
        synchronized (this) {
            this.game.pressF();
        }
    }

    public void destroy() {
        if (this.game != null) {
            this.game.clear();
        }
    }

    public void tick() {
        if (this.game != null) {
            this.game.tick();
        }
    }

    public void pressMouseLeft(boolean down) {
        this.game.pressMouseLeft(down);
    }
}
