package eu.pb4.nucledoom.mixin;

import doom.CVarManager;
import doom.ConfigManager;
import eu.pb4.nucledoom.game.DoomGame;
import mochadoom.Engine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@SuppressWarnings("ALL")
@Mixin(Engine.class)
public class EngineMixin {
    @Overwrite
    public static CVarManager getCVM() {
        var game = DoomGame.GAME.get();
        return game != null ? game.getCvarManager() : new CVarManager(List.of());
    }

    @Overwrite
    public static ConfigManager getConfig() {
        var game = DoomGame.GAME.get();
        return game != null ? game.getConfigManager() : new ConfigManager();
    }

    @Overwrite
    public static void updateFrame() {
        var game = DoomGame.GAME.get();
        if (game != null) {
            game.drawFrame();
        }
    }
}
