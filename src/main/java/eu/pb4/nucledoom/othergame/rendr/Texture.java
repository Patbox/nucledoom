package eu.pb4.nucledoom.othergame.rendr;

import eu.pb4.mapcanvas.impl.image.RawImage;

import java.awt.image.BufferedImage;

public interface Texture {
    int width();
    int height();
    int get(int x, int y);

    default int get(float x, float y) {
        return get((int) x, (int) y);
    }
    void set(int x, int y, int color);

    default boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }

    default boolean inBounds(float x, float y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }

    static Texture fromImage(BufferedImage image) {
        return new Texture() {
            @Override
            public int width() {
                return image.getWidth();
            }

            @Override
            public int height() {
                return image.getHeight();
            }

            @Override
            public int get(int x, int y) {
                return image.getRGB(x, y);
            }

            @Override
            public void set(int x, int y, int c) {
                image.setRGB(x, y, c);
            }
        };
    }

    static Texture fromRawImage(RawImage image) {
        return new Texture() {
            @Override
            public int width() {
                return image.width();
            }

            @Override
            public int height() {
                return image.height();
            }

            @Override
            public int get(int x, int y) {
                return image.get(x, y);
            }

            @Override
            public void set(int x, int y, int c) {
                image.set(x, y, c);
            }
        };
    }
}
