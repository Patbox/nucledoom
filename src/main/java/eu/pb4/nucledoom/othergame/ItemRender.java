package eu.pb4.nucledoom.othergame;

import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.mapcanvas.impl.image.RawImage;
import eu.pb4.nucledoom.PlayerSaveData;
import eu.pb4.nucledoom.game.*;
import eu.pb4.nucledoom.othergame.rendr.BlockModelRender;
import eu.pb4.nucledoom.othergame.rendr.FaceInfo;
import eu.pb4.nucledoom.othergame.rendr.RenderOutput;
import eu.pb4.nucledoom.othergame.rendr.UVs;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelElement;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelTransformation;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

public class ItemRender implements DoomGame {
    private static final String USER_AGENT = "NucleDoom/NBS Player";
    @Nullable
    private final GameHandler handler;
    private final int[] pressNum = new int[9];
    private final RenderOutput output;
    private final BlockModelRender modelRenderer;
    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;
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


    public ItemRender(@Nullable GameHandler gameHandler,
                      @Nullable PlayerSaveData saveData,
                      DoomConfig config,
                      ResourceManager resourceManager) throws IOException {
        this.handler = gameHandler;
        this.output = new RenderOutput(320, 200);
        this.modelRenderer = new BlockModelRender();
    }

    @Override
    public boolean onChat(String message) {
        Identifier parsed = Identifier.tryParse(message);
        if (parsed != null) {
            this.modelRenderer.loadModel(parsed);
            return true;
        }

        return false;
    }

    @Override
    public void startGameLoop() throws Throwable {
        while (true) {
            try {
                this.drawFrame();
            } catch (Throwable e) {
                // Ignore
            }
            if (this.close) break;
            Thread.sleep(1000 / 60);
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

        var view = new Matrix4fStack(8);

        if (this.input.left()) {
            this.yaw -= 0.01;
        }
        if (this.input.right()) {
            this.yaw += 0.01;
        }

        if (this.input.forward()) {
            this.pitch -= 0.01;
        }
        if (this.input.backward()) {
            this.pitch += 0.01;
        }

        if (this.input.jump()) {
            this.yaw = 0;
            this.pitch = 0;
            this.roll = 0;
        }

        view.translate(160, 100, 0);
        view.scale(1, -1, 1);

        view.rotateYXZ(this.yaw, this.pitch, this.roll);

        view.scale(8);

        this.output.clear();

        var vec = new Vector3f[]{
                new Vector3f(0, 0, -Float.MAX_VALUE),
                new Vector3f(0, 200, -Float.MAX_VALUE),
                new Vector3f(360, 200, -Float.MAX_VALUE),
                new Vector3f(360, 0, -Float.MAX_VALUE)
        };

        var uv = new Vector2f[]{
                new Vector2f(0, 0),
                new Vector2f(0, 16),
                new Vector2f(16, 16),
                new Vector2f(16, 0),
        };

        this.output.drawQuad(vec, uv, null, -1);

        this.modelRenderer.render(output, view);

        CanvasImage screen;
        if (this.handler.getCanvas().trueRgb()) {
            screen = new CanvasImage(this.output.width() * 2, this.output.height() * 2);

            var rgbCanvas = new RgbCanvas(screen);
            for (var x = 0; x < this.output.width(); x++) {
                for (var y = 0; y < this.output.height(); y++) {
                    rgbCanvas.setRgb(x, y, this.output.get(x, y) & 0xFFFFFF);
                }
            }

        } else {
            screen = new CanvasImage(this.output.width(), this.output.height());

            for (var x = 0; x < this.output.width(); x++) {
                for (var y = 0; y < this.output.height(); y++) {
                    screen.set(x, y, CanvasUtils.findClosestColor(this.output.get(x, y) & 0xFFFFFF));
                }
            }

        }
        this.handler.getCanvas().drawFrame(screen);
    }

    @Override
    public void updateKeyboard(Input input) {
        this.input = input;
    }

    @Override
    public void updateMouse(float xDelta, float yDelta) {

    }

    @Override
    public void pressMouseRight(boolean value) {

    }

    @Override
    public void pressMouseLeft(boolean value) {

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
