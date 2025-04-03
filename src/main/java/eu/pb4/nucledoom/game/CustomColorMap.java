package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import net.minecraft.block.MapColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

public class CustomColorMap {
    private static final byte[] RGB_TO_MAP_LEGACY = new byte[256*256*256];

    public static byte findClosestRawColor(int rgb) {
        rgb = rgb & 0xFFFFFF;
        if (RGB_TO_MAP_LEGACY[rgb] == 0) {
            RGB_TO_MAP_LEGACY[rgb] = findClosestColorMath(rgb).getRenderColor();
        }
        return RGB_TO_MAP_LEGACY[rgb];
    }

    public static void clearMap() {
        Arrays.fill(RGB_TO_MAP_LEGACY, (byte) 0);
    }


    private static CanvasColor findClosestColorMath(int rgb) {
        int shortestDistance = Integer.MAX_VALUE;
        var out = CanvasColor.CLEAR;

        final int redColor = (rgb >> 16) & 0xFF;
        final int greenColor = (rgb >> 8) & 0xFF;
        final int blueColor = rgb & 0xFF;
        final var grayscale = Math.min(ColorHelper.grayscale(rgb) & 0xFF, 0xFF);

        final var array = CanvasColor.values();
        final int length = array.length;

        for (int i = 0; i < length; i++) {
            final var canvasColor = array[i];
            if (canvasColor.getColor() == MapColor.CLEAR) {
                continue;
            }

            final int tmpColor = canvasColor.getRgbColor();

            final int redCanvas = (tmpColor >> 16) & 0xFF;
            final int greenCanvas = (tmpColor >> 8) & 0xFF;
            final int blueCanvas = (tmpColor) & 0xFF;

            final var grayscaleCanvas = ColorHelper.grayscale(tmpColor) & 0xFF;

            final int distance = MathHelper.square(grayscaleCanvas - grayscale)
                    + MathHelper.square(redCanvas - redColor)
                    + MathHelper.square(greenCanvas - greenColor)
                    + MathHelper.square(blueCanvas - blueColor);

            if (distance < shortestDistance) {
                out = canvasColor;
                shortestDistance = distance;
            }
        }

        return out;
    }
}
