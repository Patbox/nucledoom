package eu.pb4.nucledoom.game.audio;

public interface AudioController {
    AudioController NOOP = () -> {};

    void playSound();
}
