package remake.feature;

import mindustry.entities.type.Player;

import javax.annotation.Nullable;

public class Vote {
    Player target;
    String reason;
    boolean status = false;

    enum Type {
        map, gameover, skipwave, gamemode, custom
    }

    public void start(Type type, @Nullable Player target) {

    }

    public void success() {

    }

    public void interrupt() {

    }

    public boolean status() {
        return status;
    }

    public void get() {

    }

    public void set() {

    }

    public void result() {

    }
}
