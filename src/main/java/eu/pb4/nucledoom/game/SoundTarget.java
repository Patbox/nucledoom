package eu.pb4.nucledoom.game;

public enum SoundTarget {
    MUSIC_ANY,
    SFX_VANILLA,
    MUSIC_VANILLA,
    MUSIC_EXT,
    SFX_EXT;

    public boolean isVanilla() {
        return this == SFX_VANILLA || this == MUSIC_VANILLA;
    }

    public boolean isSupported(boolean sfxExt, boolean musicExt) {
        return this == MUSIC_ANY || this == (sfxExt ? SFX_EXT : SFX_VANILLA) || this == (musicExt ? MUSIC_EXT : MUSIC_VANILLA);
    }
}
