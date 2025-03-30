package eu.pb4.nucledoom.mixin;

import doom.CVarManager;
import doom.DoomMain;
import eu.pb4.nucledoom.game.doom.DoomGame;
import eu.pb4.nucledoom.game.doom.MinecraftSoundDriver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import s.DummySFX;
import s.ISoundDriver;

@SuppressWarnings("ALL")
@Mixin(value = ISoundDriver.class, remap = false)
public interface ISoundDriverMixin {
    @Overwrite(remap = false)
    static ISoundDriver chooseModule(DoomMain<?, ?> DM, CVarManager CVM) {
        var game = DoomGame.GAME.get();
        if (game != null) {
            return new MinecraftSoundDriver(DM, game);
        }

        return new DummySFX();
    }
}
