package eu.pb4.doomwrapper;

import data.sounds;
import doom.DoomMain;
import eu.pb4.nucledoom.game.SoundTarget;
import s.AbstractSoundDriver;

import java.util.Random;

public class MinecraftSoundDriver extends AbstractSoundDriver {
    private final DoomGameImpl game;

    public MinecraftSoundDriver(DoomMain<?, ?> DM, DoomGameImpl doomGame) {
        super(DM, 999);
        this.game = doomGame;
    }

    @Override
    protected int addsfx(int sfxid, int volume, int step, int seperation) {
        var sourceSound = sounds.S_sfx[sfxid];
        var pitch = new Random().nextFloat(0.85f, 1.15f);
        var vol = volume / 128f;
        if (this.game.supportsSoundTarget(SoundTarget.SFX_EXT)) {
            this.game.playSound(SoundTarget.SFX_EXT, SoundMap.DOOM_MAP.get(sourceSound.name), pitch, vol);
        }
        if (this.game.supportsSoundTarget(SoundTarget.SFX_VANILLA)) {
            var sounds = SoundMap.MAP.get(sourceSound.name);
            if (sounds != null) {
                for (var sound : sounds) {
                    this.game.playSound(SoundTarget.SFX_VANILLA, sound.event(), pitch * sound.pitch(), vol * sound.volume());
                }
            } else {
                System.out.println("Missing sound " + sourceSound.name);
            }
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
