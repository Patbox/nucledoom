package eu.pb4.nucledoom.mixin;

import eu.pb4.nucledoom.game.doom.DoomGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import utils.C2JUtils;
import w.InputStreamSugar;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(value = C2JUtils.class, remap = false)
public abstract class C2JUtilsMixin {
    @Shadow
    public static boolean checkForExtension(String filename, String ext) {
        return false;
    }

    @Overwrite(remap = false)
    public static int guessResourceType(String uri) {
        var game = DoomGame.GAME.get();
        if (game == null || uri == null || uri.isEmpty()) {
            return InputStreamSugar.BAD_URI;
        }

        int result = 0;

        if (game.getResourceStream(uri) == null) {
            return InputStreamSugar.BAD_URI;
        }
        result |= InputStreamSugar.FILE;

        if (checkForExtension(uri, "zip")) {
            result |= InputStreamSugar.ZIP_FILE;

        }
        // All is well. Go on...
        return result;
    }

    @Overwrite(remap = false)
    public static boolean testReadAccess(String uri) {
        var game = DoomGame.GAME.get();
        if (game == null || uri == null || uri.isEmpty()) {
            return false;
        }

        return game.getResourceStream(uri) != null;
    }

    @Overwrite(remap = false)
    public static boolean testWriteAccess(String uri) {
        return false;
    }

}
