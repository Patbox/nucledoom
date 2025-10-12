package eu.pb4.nucledoom.game;

import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.PlayerSaveData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.PlayerInput;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public interface DoomGame {
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

    void updateKeyboard(PlayerInput input);

    void updateMouse(float xDelta, boolean mouseLeft);

    void selectSlot(int selectedSlot);

    void pressE();

    void pressQ();

    void pressF();

    void tick();

    void extractAudio(BiConsumer<String, byte[]> consumer);

    record Open(DoomGame game, JarGameClassLoader loader) {
    }
}
