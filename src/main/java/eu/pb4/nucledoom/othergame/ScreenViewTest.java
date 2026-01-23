package eu.pb4.nucledoom.othergame;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.mapcanvas.impl.image.RawImage;
import eu.pb4.nucledoom.PlayerSaveData;
import eu.pb4.nucledoom.game.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Input;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.function.BiConsumer;

public class ScreenViewTest implements DoomGame {
    @Nullable
    private final GameHandler handler;
    private final int[] pressNum = new int[9];
    private final Robot robot;
    private final ScreenInfo screenInfo;
    private final Dimension screenSize;
    private final Rectangle rectangle;
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

    private int mouseX;
    private int mouseY;

    public ScreenViewTest(@Nullable GameHandler gameHandler,
                          @Nullable PlayerSaveData saveData,
                          DoomConfig config,
                          ResourceManager resourceManager) throws Throwable {
        this.handler = gameHandler;
        this.robot = new Robot();
        this.screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenInfo = new ScreenInfo(screenSize.width, screenSize.height, ScreenInfo.DEFAULT.background(), ScreenInfo.DEFAULT.overlay(), ScreenInfo.DEFAULT.overlayReset(), 1);
        this.rectangle = new Rectangle(this.screenSize);
    }

    @Override
    public ScreenInfo getScreenInfo() {
        return this.screenInfo;
    }

    @Override
    public boolean onChat(String message) {

        return false;
    }

    @Override
    public void startGameLoop() throws Throwable {
        try {
            while (true) {
                this.drawFrame();
                if (this.close) break;
                Thread.sleep(100);
            }
        } finally {

        }
    }

    @Override
    public void clear() {

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
            this.screen = new CanvasImage(this.screenInfo.width(), this.screenInfo.height());
        }
        var rgb = this.screen;
        var time = System.currentTimeMillis();
        var image = RawImage.convert(this.robot.createScreenCapture(this.rectangle));


        for (var x = 0; x < this.screenSize.width; x++) {
            for (var y = 0; y < this.screenSize.height; y++) {
                rgb.set(x, y, CanvasUtils.findClosestColor(image.get(x, y)));
            }
        }
        CanvasUtils.fill(rgb, this.mouseX - 3, this.mouseY - 3, this.mouseX + 3, this.mouseY + 3, CanvasColor.WHITE_HIGH);
        rgb.set(this.mouseX, this.mouseY, CanvasColor.RED_HIGH);

        DefaultFonts.VANILLA.drawText(rgb, String.valueOf(System.currentTimeMillis() - time), 8,8, 8, CanvasColor.WHITE_HIGH);
        this.handler.getCanvas().drawFrame(this.screen);
    }

    @Override
    public void updateKeyboard(Input input) {
        if (input.forward()) {
            this.pressForward = 10;
        }

        if (input.backward()) {
            this.pressBackward = 10;
        }

        this.input = input;
    }

    @Override
    public void updateMouse(float xDelta, float yDelta) {
        double d = 0.6000000238418579 + 0.20000000298023224;
        double e = d * d * d;
        double f = e * 8.0;

        var newX = this.mouseX + (int) (xDelta * 6 / 0.15 / f);
        var newY = this.mouseY + (int) (yDelta * 6 / 0.15 / f);
        if (this.mouseX != newX || this.mouseY != newY) {
            this.robot.mouseMove(newX, newY);
            this.mouseX = newX;
            this.mouseY = newY;
        }
    }

    @Override
    public void pressMouseLeft(boolean value) {
        if (value) {
            this.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        } else {
            this.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void pressMouseRight(boolean value) {
        if (value) {
            this.robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        } else {
            this.robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        }
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
        return "";
    }
}
