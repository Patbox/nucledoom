package eu.pb4.nucledoom.mixin;

import doom.CVarManager;
import eu.pb4.nucledoom.game.doom.DoomGame;
import eu.pb4.nucledoom.game.doom.MinecraftMusicDriver;
import eu.pb4.nucledoom.game.doom.MinecraftSoundDriver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import s.DummyMusic;
import s.DummySFX;
import s.IMusic;

@SuppressWarnings("ALL")
@Mixin(value = IMusic.class, remap = false)
public interface IMusicMixin {
    @Overwrite(remap = false)
    static IMusic chooseModule(CVarManager CVM) {
        var game = DoomGame.GAME.get();
        if (game != null) {
            return new MinecraftMusicDriver(game);
        }

        return new DummyMusic();
    }
}
