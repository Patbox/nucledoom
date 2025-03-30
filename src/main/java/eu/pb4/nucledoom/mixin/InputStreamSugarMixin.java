package eu.pb4.nucledoom.mixin;

import eu.pb4.nucledoom.game.doom.DoomGame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import utils.C2JUtils;
import w.InputStreamSugar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(value = InputStreamSugar.class, remap = false)
public abstract class InputStreamSugarMixin {

    @Shadow
    protected static InputStream getZipEntryStream(ZipInputStream zis, String entryname) {
        return null;
    }

    @Shadow @Final public static int ZIP_FILE;

    @Overwrite(remap = false)
    public static final InputStream createInputStreamFromURI(String resource, ZipEntry entry, int type) {
        InputStream is = null;
        URL u;

        if (entry == null || !C2JUtils.flags(type, ZIP_FILE)) {
            is = getDirectInputStream(resource);
        } else {
            if (C2JUtils.flags(type, ZIP_FILE)) {
                ZipInputStream zis;

                try {
                    zis = new ZipInputStream(getDirectInputStream(resource));
                } catch (Exception e1) {
                    return getDirectInputStream(resource);
                }

                is = getZipEntryStream(zis, entry.getName());
                if (is != null) {
                    return is;
                }
            }
        }

        return getDirectInputStream(resource);
    }

    @Overwrite(remap = false)
    private final static InputStream getDirectInputStream(String resource) {
        var game = DoomGame.GAME.get();
        if (game == null) {
            return null;
        }

        var out = game.getResourceStream(resource);

        return out != null ? out.get() : null;
    }

    @Overwrite(remap = false)
    public static final InputStream streamSeek(InputStream is, long pos, long size, String uri, ZipEntry entry, int type) throws IOException {
        if (is == null) {
            return is;
        } else {
            if (size > 0L) {
                try {
                    long available = (long)is.available();
                    long guesspos = size - available;
                    if (guesspos > 0L && guesspos <= pos) {
                        long skipped = 0L;

                        for(long mustskip = pos - guesspos; skipped < mustskip; skipped += is.skip(mustskip - skipped)) {
                        }

                        return is;
                    }
                } catch (Exception var18) {
                }
            }

            is.close();
            is = createInputStreamFromURI(uri, is instanceof ZipInputStream ? entry : null, type);
            is.skip(pos);
            return is;
        }
    }
}
