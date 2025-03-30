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
    public static final CanvasFont OPEN_ZOO_4x8;

    static {
        {
            CanvasFont openZoo4x6;
            CanvasFont openZoo4x8;

            var texture4x6 = FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID)
                    .get().findPath("map/openzoo/4x6.png").get();
            var texture4x8 = FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID)
                    .get().findPath("map/openzoo/4x8.png").get();
            var emptyGlyph = BitmapFontBuilder.Glyph.of(1, 1);

            var b = new byte[256];

            for (int i = 0; i < 256; i++) {
                b[i] = (byte) i;
            }

            try {
                var builder4x6 = BitmapFontBuilder.create();
                var builder4x8 = BitmapFontBuilder.create();

                buildFontZoo(texture4x6, builder4x6, 4, 6, b);
                buildFontZoo(texture4x8, builder4x8, 4, 8, b);

                openZoo4x6 = builder4x6.defaultGlyph(emptyGlyph).build();
                openZoo4x8 = builder4x8.defaultGlyph(emptyGlyph).build();
            } catch (Throwable e) {
                e.printStackTrace();
                openZoo4x6 = openZoo4x8 = DefaultFonts.VANILLA;
            }

            OPEN_ZOO_4x6 = openZoo4x6;
            OPEN_ZOO_4x8 = openZoo4x8;
        }
    }
    
    private static void buildFontZoo(Path texturePath, BitmapFontBuilder builder, int fontWidth, int fontHeight, byte[] set) throws IOException {

        var image = ImageIO.read(Files.newInputStream(texturePath));

        for (int i = 0; i < 256; i++) {
            int column = i % 32;
            int row = i / 32;

            int xStart = column * fontWidth;
            int yStart = row * fontHeight;

            var glyph = BitmapFontBuilder.Glyph.of(fontWidth, fontHeight).logicalHeight(fontHeight);

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
