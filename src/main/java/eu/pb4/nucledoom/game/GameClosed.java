package eu.pb4.nucledoom.game;

public class GameClosed extends RuntimeException {
    private final int status;

    public GameClosed(int status) {
        this.status = status;
    }


    public int status() {
        return status;
    }
}
