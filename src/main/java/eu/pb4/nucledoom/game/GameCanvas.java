package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.core.*;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.nucledoom.ExtraFonts;
import eu.pb4.nucledoom.NucleDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GameCanvas {
    private static final Logger LOGGER = LoggerFactory.getLogger("GameCanvas");

    private static final int MAP_SIZE = MapItem.IMAGE_WIDTH;
    private final String title;
    private final boolean trueRgb;
    private final DrawableCanvas drawCanvas;
    private final int screenWidth;
    private final int screenHeight;
    private final int sectionHeight;
    private final int sectionWidth;
    private final int drawOffsetX;
    private final int drawOffsetY;
    private final CombinedPlayerCanvas canvas;
    private String controls;
    private long previousFrameTime = -1;
    private CanvasImage background;
    private CanvasImage overlay;
    private CanvasImage overlayReset;
    private int backgroundScale = 1;

    public GameCanvas(String title, boolean trueRgb, DoomGame.ScreenInfo screenInfo, String controls) {
        this.title = title;
        this.trueRgb = trueRgb;
        this.screenHeight = screenInfo.height();
        this.screenWidth = screenInfo.width();
        this.sectionHeight = (5 * 128 - 200 + screenHeight) / 128;
        this.sectionWidth = (8 * 128 - 360 + screenWidth) / 128;
        this.drawOffsetX = sectionWidth * 64 - screenWidth / 2;
        this.drawOffsetY = sectionHeight * 64 - screenHeight / 2;
        var trueRgbScale = this.trueRgb ? 2 : 1;

        this.canvas = DrawableCanvas.create(sectionWidth * trueRgbScale, sectionHeight * trueRgbScale);
        this.drawCanvas = this.trueRgb ? new RgbCanvas(this.canvas) : this.canvas;
        this.controls = controls;
        this.updateBackgroundTexture(screenInfo.background(), screenInfo.overlay(),  screenInfo.overlayReset(), screenInfo.backgroundScale());
        this.drawBackground();
    }

    public void updateBackgroundTexture(Identifier background, Identifier overlay, Identifier overlayReset, int backgroundScale) {
        this.updateBackgroundTexture(NucleDoom.BACKGROUND.get(background), NucleDoom.BACKGROUND.get(overlay), NucleDoom.BACKGROUND.get(overlayReset), backgroundScale);
    }

    public void updateBackgroundTexture(CanvasImage background, CanvasImage overlay, CanvasImage overlayReset, int backgroundScale) {
        this.background = background;
        this.overlay = overlay;
        this.overlayReset = overlayReset;
        this.backgroundScale = backgroundScale;
    }

    public void setControls(String controls) {
        this.controls = controls;
    }

    public void drawBackground() {
        CanvasUtils.clear(this.drawCanvas, CanvasColor.CLEAR);
        if (background != null) {
            var width = background.getWidth() * backgroundScale;
            var height = background.getHeight() * backgroundScale;
            var repeatsX = Math.ceilDiv(this.drawCanvas.getWidth(), width);
            var repeatsY = Math.ceilDiv(this.drawCanvas.getHeight(), height);

            for (int x = 0; x < repeatsX; x++) {
                for (int y = 0; y < repeatsY; y++) {
                    CanvasUtils.draw(this.drawCanvas, x * width, y * height, width, height, background);
                }
            }
        }

        if (overlay != null) {
            var background = overlay;
            var width = background.getWidth() * backgroundScale;
            var height = background.getHeight() * backgroundScale;
            CanvasUtils.draw(this.drawCanvas, this.drawCanvas.getWidth() / 2 - width / 2, this.drawCanvas.getHeight() / 2 - height / 2, width, height, background);
        }

        CanvasUtils.fill(this.drawCanvas, drawOffsetX, drawOffsetY, drawOffsetX + screenWidth, drawOffsetY + screenHeight, CanvasColor.BLACK_NORMAL);

        DefaultFonts.UNIFONT.drawText(this.drawCanvas, this.title, drawOffsetX + 2, drawOffsetY - 16 - 4, 16, CanvasColor.WHITE_HIGH);

        ExtraFonts.OPEN_ZOO_4x8.drawText(this.drawCanvas, controls, drawOffsetX - 91 * backgroundScale, drawOffsetY + 70 * backgroundScale, 8 * backgroundScale, CanvasColor.BLACK_HIGH);
    }

    public void drawError(Throwable e) {
        var width = DefaultFonts.UNIFONT.getTextWidth("ERROR!", 16);

        CanvasUtils.fill(this.drawCanvas, (screenWidth - width) / 2 - 5 + drawOffsetX, 11 + drawOffsetY,
                (screenWidth - width) / 2 + width + 5 + drawOffsetX, 16 * 2 + 5 + drawOffsetY, CanvasColor.BLUE_HIGH);
        //CanvasUtils.fill(this.drawCanvas, 0, 0, SCREEN_HEIGHT, SCREEN_WIDTH, CanvasColor.BLUE_HIGH);
        DefaultFonts.UNIFONT.drawText(this.drawCanvas, "ERROR!", (screenWidth - width) / 2 + 1 + drawOffsetX, 17 + drawOffsetY, 16, CanvasColor.BLACK_LOW);
        DefaultFonts.UNIFONT.drawText(this.drawCanvas, "ERROR!", (screenWidth - width) / 2 + drawOffsetX, 16 + drawOffsetY, 16, CanvasColor.RED_HIGH);

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

        CanvasUtils.fill(this.drawCanvas, drawOffsetX, 63 + drawOffsetY,
                screenWidth + drawOffsetX, 65 + 8 + drawOffsetY, CanvasColor.BLUE_HIGH);

        DefaultFonts.VANILLA.drawText(canvas, message1, 5 + drawOffsetX, 64 + drawOffsetY, 8, CanvasColor.WHITE_HIGH);

        CanvasUtils.fill(this.drawCanvas, drawOffsetX, 63 + 10 + drawOffsetY,
                screenWidth + drawOffsetX, 65 + 10 + message2Split.size() * 10 + drawOffsetY, CanvasColor.BLUE_HIGH);
        for (int i = 0; i < message2Split.size(); i++) {
            DefaultFonts.VANILLA.drawText(canvas, message2Split.get(i), 5 + drawOffsetX, 64 + 10 + 10 * i + drawOffsetY, 8, CanvasColor.WHITE_HIGH);
        }

        this.canvas.sendUpdates();
    }

    public BlockPos getDisplayPos() {
        return new BlockPos(-sectionWidth, sectionHeight + 100, 0);
    }

    public Vec3 getSpawnPos() {
        BlockPos displayPos = this.getDisplayPos();
        var trueRgbScale = this.trueRgb ? 2 : 1;

        return new Vec3(displayPos.getX() + sectionWidth * 0.5 * trueRgbScale, displayPos.getY() - sectionHeight * 0.5f * trueRgbScale + 1, 1.5 *  this.screenWidth / 320 * trueRgbScale + 0.01f);
    }


    public PlayerCanvas getCanvas() {
        return this.canvas;
    }

    public void drawFrame(DrawableCanvas canvas) {
        var trueRgbScale = this.trueRgb ? 2 : 1;
        var frame = System.currentTimeMillis();

        if (overlayReset != null) {
            var background = overlayReset;
            var width = background.getWidth() * backgroundScale;
            var height = background.getHeight() * backgroundScale;

            CanvasUtils.draw(this.drawCanvas, this.drawCanvas.getWidth() / 2 - width / 2, this.drawCanvas.getHeight() / 2 - height / 2, width, height,
                    background);
        }
        var text = String.format("%s - %s MS", this.title, frame - previousFrameTime);
        DefaultFonts.UNIFONT.drawText(this.drawCanvas, text, drawOffsetX + 2 * backgroundScale, drawOffsetY - (16 + 4) * backgroundScale, 16 * backgroundScale, CanvasColor.WHITE_HIGH);

        CanvasUtils.draw(this.canvas, drawOffsetX * trueRgbScale, drawOffsetY * trueRgbScale, canvas);

        previousFrameTime = frame;
        this.canvas.sendUpdates();
    }

    public boolean trueRgb() {
        return this.trueRgb;
    }

    public void close() {
        this.canvas.destroy();
    }

    public int getScreenWidth() {
        return this.screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}
