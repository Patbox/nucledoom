package eu.pb4.nucledoom;

import eu.pb4.mapcanvas.api.font.BitmapFontBuilder;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExtraFonts {
    public static final CanvasFont OPEN_ZOO_4x6;

    static {
        {
            CanvasFont openZoo4x6;

            var texturePathZoo = FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID)
                    .get().findPath("map/openzoo/4x6.png").get();
            var emptyGlyph = BitmapFontBuilder.Glyph.of(1, 1);

            var b = new byte[256];

            for (int i = 0; i < 256; i++) {
                b[i] = (byte) i;
            }

            try {
                var builderSmall = BitmapFontBuilder.create();

                buildFontZoo(texturePathZoo, builderSmall, 4, 6, b);

                openZoo4x6 = builderSmall.defaultGlyph(emptyGlyph).build();
            } catch (Throwable e) {
                e.printStackTrace();
                openZoo4x6 = DefaultFonts.VANILLA;
            }

            OPEN_ZOO_4x6 = openZoo4x6;
        }
    }
    
    private static void buildFontZoo(Path texturePath, BitmapFontBuilder builder, int fontWidth, int fontHeight, byte[] set) throws IOException {

        var image = ImageIO.read(Files.newInputStream(texturePath));

        for (int i = 0; i < 256; i++) {
            int column = i % 32;
            int row = i / 32;

            int xStart = column * fontWidth;
            int yStart = row * fontHeight;

            var glyph = BitmapFontBuilder.Glyph.of(fontWidth, fontHeight).logicalHeight(fontHeight).charWidth(fontWidth);

            for (int x = 0; x < fontWidth;  x++) {
                for (int y = 0; y < fontHeight; y++) {
                    if (image.getRGB(xStart + x, yStart + y) == 0xFFFFFFFF) {
                        glyph.set(x, y);
                    }
                }
            }
            builder.put(Byte.toUnsignedInt(set[i]), glyph);
        }
    }

    public static void load() {
    }
}
