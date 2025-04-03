package eu.pb4.doomwrapper;

import eu.pb4.mapcanvas.api.core.DrawableCanvas;

public record FastCanvasImage(int width, int height, byte[] data) implements DrawableCanvas {
    public FastCanvasImage(int width, int height) {
        this(width, height, new byte[width * height]);
    }

    @Override
    public byte getRaw(int x, int y) {
        return data[x + y * width];
    }

    @Override
    public void setRaw(int x, int y, byte color) {
        data[x + y * width] = color;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }
}
