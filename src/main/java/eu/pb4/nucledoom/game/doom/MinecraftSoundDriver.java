package eu.pb4.nucledoom.game.doom;

import data.sounds;
import doom.DoomMain;
import net.minecraft.sound.SoundEvents;
import s.AbstractSoundDriver;

import java.util.Random;

public class MinecraftSoundDriver extends AbstractSoundDriver {
    private final DoomGame game;

    public MinecraftSoundDriver(DoomMain<?, ?> DM, DoomGame doomGame) {
        super(DM, 999);
        this.game = doomGame;
    }

    @Override
    protected int addsfx(int sfxid, int volume, int step, int seperation) {
        var sourceSound = sounds.S_sfx[sfxid];
        var sounds = SoundMap.MAP.get(sourceSound.name);

        if (sounds != null) {
            var pitch = new Random().nextFloat(0.85f, 1.15f);
            var vol = volume / 64f;
            for (var sound : sounds) {
                this.game.playSound(sound.event(), pitch * sound.pitch(), vol * sound.volume());
            }
        } else {
            System.out.println("Missing sound " + sourceSound.name);
        }

        return 0;
    }

    @Override
    public boolean InitSound() {
        return true;
    }

    @Override
    public void UpdateSound() {}

    @Override
    public void SubmitSound() {}

    @Override
    public void ShutdownSound() {}

    @Override
    public void SetChannels(int i) {

    }

    @Override
    public void StopSound(int i) {}

    @Override
    public boolean SoundIsPlaying(int i) {
        return false;
    }

    @Override
    public void UpdateSoundParams(int i, int i1, int i2, int i3) {}
}
