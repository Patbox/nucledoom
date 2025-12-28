package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import net.minecraft.util.ARGB;

public record RgbCanvas(DrawableCanvas canvas) implements DrawableCanvas {
    @Override
    public byte getRaw(int x, int y) {
        return CanvasUtils.findClosestRawColor(getRgb(x, y));
    }

    @Override
    public void setRaw(int x, int y, byte b) {
        setRgb(x, y, CanvasColor.getFromRaw(b).getRgbColor());
    }

    public int getRgb(int x, int y) {
        var b1 = RgbColorUtil.INDEX_2_DATA[this.canvas.getRaw(x * 2, y * 2)];
        var b2 = RgbColorUtil.INDEX_2_DATA[this.canvas.getRaw(x * 2 + 1, y * 2)];
        var b3 = RgbColorUtil.INDEX_2_DATA[this.canvas.getRaw(x * 2, y * 2 + 1)];
        var b4 = RgbColorUtil.INDEX_2_DATA[this.canvas.getRaw(x * 2 + 1, y * 2 + 1)];

        b1 |= (b4 & 1) << 7; b2 |= (b4 & 2) << 6; b3 |= (b4 & 4) << 5;
        return ARGB.color(b3, b2, b1);
    }

    public void setRgb(int x, int y, int d) {
        var b1 = d & 0xFF; var msb1 = b1 >> 7;
        var b2 = (d >> 8) & 0xFF; var msb2 = b2 >> 7;
        var b3 = (d >> 16) & 0xFF; var msb3 = b3 >> 7;
        var b4 = (msb3 << 2) | (msb2 << 1) | msb1;

        b1 &= 0x7F; b2 &= 0x7F; b3 &= 0x7F;
        this.canvas.setRaw(x * 2, y * 2, RgbColorUtil.INDEX_2_DATA[b1]);
        this.canvas.setRaw(x * 2 + 1, y * 2, RgbColorUtil.INDEX_2_DATA[b2]);
        this.canvas.setRaw(x * 2, y * 2 + 1, RgbColorUtil.INDEX_2_DATA[b3]);
        this.canvas.setRaw(x * 2 + 1, y * 2 + 1, RgbColorUtil.INDEX_2_DATA[b4]);
    }

    /*public static void setRgb(byte[] image, int width, int x, int y, int d) {
        var b1 = d & 0xFF; var msb1 = b1 >> 7;
        var b2 = (d >> 8) & 0xFF; var msb2 = b2 >> 7;
        var b3 = (d >> 16) & 0xFF; var msb3 = b3 >> 7;
        var b4 = (msb3 << 2) | (msb2 << 1) | msb1;

        b1 &= 0x7F; b2 &= 0x7F; b3 &= 0x7F;
        image[x * 2 + y * 2 * width] = RgbColorUtil.INDEX_2_DATA[b1];
        image[x * 2 + 1 + y * 2 * width] = RgbColorUtil.INDEX_2_DATA[b2];
        image[x * 2 + (y * 2 + 1) * width] = RgbColorUtil.INDEX_2_DATA[b3];
        image[x * 2 + 1 + (y * 2 + 1) * width] = RgbColorUtil.INDEX_2_DATA[b4];
    }*/

    @Override
    public int getHeight() {
        return canvas.getHeight() / 2;
    }

    @Override
    public int getWidth() {
        return canvas.getWidth() / 2;
    }
}
