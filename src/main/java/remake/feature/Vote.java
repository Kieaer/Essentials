package remake.feature;

import mindustry.entities.type.Player;

import java.util.ArrayList;
import java.util.List;

public class Vote {
    Player target;
    String reason;
    List<String> voted = new ArrayList<>();

    boolean status = false;
    int require;

    public int getRequire() {
        return require;
    }

    public enum VoteType {
        map, gameover, skipwave, gamemode, custom, kick
    }

    public void start(VoteType type, Player player, Object... parameters) {

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

    public List<String> getVoted() {
        return voted;
    }

    public void set() {

    }

    public void result() {

    }
}
