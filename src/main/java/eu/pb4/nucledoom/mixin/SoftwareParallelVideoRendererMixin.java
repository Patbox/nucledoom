package eu.pb4.nucledoom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;

@Mixin(targets = "v/renderers/SoftwareParallelVideoRenderer", remap = false)
public class SoftwareParallelVideoRendererMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/awt/GraphicsEnvironment;getDefaultScreenDevice()Ljava/awt/GraphicsDevice;"), remap = false)
    private static GraphicsDevice fakeIt(GraphicsEnvironment instance) {
        return null;
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/awt/GraphicsDevice;getDefaultConfiguration()Ljava/awt/GraphicsConfiguration;"), remap = false)
    private static GraphicsConfiguration tillYouMakeIt(GraphicsDevice instance) {
        return new GraphicsConfiguration() {
            @Override
            public GraphicsDevice getDevice() {
                return null;
            }

            @Override
            public ColorModel getColorModel() {
                return ColorModel.getRGBdefault();
            }

            @Override
            public ColorModel getColorModel(int transparency) {
                return ColorModel.getRGBdefault();
            }

            @Override
            public AffineTransform getDefaultTransform() {
                return new AffineTransform();
            }

            @Override
            public AffineTransform getNormalizingTransform() {
                return new AffineTransform();
            }

            @Override
            public Rectangle getBounds() {
                return new Rectangle();
            }
        };
    }
}
