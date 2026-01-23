package eu.pb4.nucledoom.game;

import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.PlayerSaveData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Input;
import org.jetbrains.annotations.Nullable;
import oshi.driver.mac.WindowInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public interface DoomGame {
    interface GameOpener {
        Open createGame(@Nullable GameHandler handler, @Nullable PlayerSaveData saveData, DoomConfig config, ResourceManager resourceManager) throws Throwable;
    }


    static Open create(@Nullable GameHandler handler, @Nullable PlayerSaveData saveData, DoomConfig config, ResourceManager resourceManager) throws Throwable {
        List<Path> path = null;
        var base = FabricLoader.getInstance().getGameDir();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            path = List.of(base.resolve("../doomwrapper/build/devlibs/doomwrapper-dev.jar"),
                    base.resolve("../jars/mochadoom.jar"));
        } else {
            var container = FabricLoader.getInstance().getModContainer(NucleDoom.MOD_ID).get();
            var wrapper = container.findPath("jars/doomwrapper.jar").get();
            var doom = container.findPath("jars/mochadoom.jar").get();
            if (Files.exists(base.resolve("nucledoom_override/doomwrapper.jar"))) {
                wrapper = base.resolve("nucledoom_override/doomwrapper.jar");
            }
            if (Files.exists(base.resolve("nucledoom_override/mochadoom.jar"))) {
                doom = base.resolve("nucledoom_override/mochadoom.jar");
            }
            path = List.of(wrapper, doom);
        }

        var loader = new JarGameClassLoader(path);
        return new Open(
                (DoomGame) loader.findClass("eu.pb4.doomwrapper.DoomGameImpl")
                        .getConstructor(GameHandler.class, PlayerSaveData.class, DoomConfig.class, ResourceManager.class)
                        .newInstance(handler, saveData, config, resourceManager),
                loader);
    }

    void startGameLoop() throws Throwable;

    void clear();

    void updateKeyboard(Input input);

    void updateMouse(float xDelta, float yDelta);
    void pressMouseRight(boolean value);
    void pressMouseLeft(boolean value);


    void selectSlot(int selectedSlot);

    boolean onChat(String message);

    void pressE();

    void pressQ();

    void pressF();

    void tick();

    void extractAudio(BiConsumer<String, byte[]> consumer);

    String getControls();

    default ScreenInfo getScreenInfo() {
        return ScreenInfo.DEFAULT;
    }

    record Open(DoomGame game, JarGameClassLoader loader) {}


    record ScreenInfo(int width, int height, Identifier background, Identifier overlay, Identifier overlayReset, int backgroundScale) {
        public static final ScreenInfo DEFAULT = new ScreenInfo(320, 200,
                Identifier.fromNamespaceAndPath("nucledoom", "default_background"),
                Identifier.fromNamespaceAndPath("nucledoom", "default_overlay"),
                Identifier.fromNamespaceAndPath("nucledoom", "default_overlay_reset"),
                1
        );

        public static final ScreenInfo FALLBACK = new ScreenInfo(320, 200,
                Identifier.fromNamespaceAndPath("nucledoom", "default_background"),
                Identifier.fromNamespaceAndPath("nucledoom", "empty"),
                Identifier.fromNamespaceAndPath("nucledoom", "empty"),
                1
        );

        public ScreenInfo scale(int scale) {
            return new ScreenInfo(width * scale, height * scale, background, overlay, overlayReset, this.backgroundScale * scale);
        }
    }
}
