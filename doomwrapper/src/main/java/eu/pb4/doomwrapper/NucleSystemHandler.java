package eu.pb4.doomwrapper;

import doom.CVarManager;
import doom.ConfigManager;
import doom.DoomMain;
import doom.event_t;
import eu.pb4.nucledoom.game.GameClosed;
import mochadoom.SystemHandler;
import s.DummyMusic;
import s.DummySFX;
import s.IMusic;
import s.ISoundDriver;
import utils.C2JUtils;
import w.InputStreamSugar;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static utils.C2JUtils.checkForExtension;
import static w.InputStreamSugar.ZIP_FILE;
import static w.InputStreamSugar.getZipEntryStream;

public record NucleSystemHandler(DoomGameImpl game) implements SystemHandler.Impl {
    @Override
    public boolean allowSaves() {
        return false;
    }

    @Override
    public int guessResourceType(String uri) {
        if (uri == null || uri.isEmpty()) {
            return InputStreamSugar.BAD_URI;
        }

        int result = 0;

        if (game.getResourceStream(uri) == null) {
            return InputStreamSugar.BAD_URI;
        }
        result |= InputStreamSugar.FILE;

        if (checkForExtension(uri, "zip")) {
            result |= ZIP_FILE;

        }
        // All is well. Go on...
        return result;
    }

    @Override
    public boolean testReadAccess(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }

        return game.getResourceStream(uri) != null;
    }

    @Override
    public boolean testWriteAccess(String s) {
        return false;
    }

    @Override
    public void systemExit(int status) {
        throw new GameClosed(status);
    }

    @Override
    public CVarManager getCvars() {
        return game.getCvarManager();
    }

    @Override
    public ConfigManager getConfig() {
        return game.getConfigManager();
    }

    @Override
    public void updateFrame() {
        game.drawFrame();
    }

    @Override
    public IMusic chooseMusicModule(CVarManager cVarManager) {
        return new MinecraftMusicDriver(game);
    }

    @Override
    public ISoundDriver chooseSoundModule(DoomMain<?, ?> doomMain, CVarManager cVarManager) {
        return new MinecraftSoundDriver(doomMain, game);
    }

    @Override
    public InputStream createInputStreamFromURI(String resource, ZipEntry entry, int type) {
        if (entry != null && C2JUtils.flags(type, ZIP_FILE)) {
            ZipInputStream zis;

            try {
                zis = new ZipInputStream(getDirectInputStream(resource));
            } catch (Exception e1) {
                return getDirectInputStream(resource);
            }

            var is = getZipEntryStream(zis, entry.getName());
            if (is != null) {
                return is;
            }
        }

        return getDirectInputStream(resource);
    }

    @Override
    public InputStream getDirectInputStream(String resource) {
        var out = game.getResourceStream(resource);

        return out != null ? out.get() : null;
    }

    @Override
    public InputStream streamSeek(InputStream is, long pos, long size, String uri, ZipEntry entry, int type) throws IOException {
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

    @Override
    public void resetIn(event_t.mouseevent_t mouseeventT, Robot robot, Point point, int i, int i1) {

    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
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

    @Override
    public boolean fileExists(String s) {
        return this.game.getResourceStream(s) != null;
    }

    @Override
    public BufferedReader getFileBufferedReader(String s, Charset charset) throws IOException {
        var file = getDirectInputStream(s);

        return new BufferedReader(file != null ? new InputStreamReader(file, charset) : Reader.nullReader());
    }

    @Override
    public BufferedWriter getFileBufferedWriter(String s, Charset charset, OpenOption[] openOptions) throws IOException {
        return new BufferedWriter(Writer.nullWriter());
    }

    @Override
    public void mainLoopStart() {
        this.game.mainLoopStart();
    }

    @Override
    public void mainLoopPostTic() {

    }

    @Override
    public void mainLoopEnd() {

    }
}
