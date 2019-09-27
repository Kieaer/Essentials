package essentials;

import io.anuke.arc.util.Log;

public class Global {
    public static void log(String msg){
        Log.info("[Essentials] "+msg);
    }

    public static void banc(String msg){
        Log.info("[EssentialsBanClient] "+msg);
    }

    public static void bans(String msg){
        Log.info("[EssentialsBanServer] "+msg);
    }

    public static void bansw(String msg){
        Log.warn("[EssentialsBanServer] "+msg);
    }

    public static void chatc(String msg){
        Log.info("[EssentialsChatClient] "+msg);
    }

    public static void chats(String msg){
        Log.info("[EssentialsChatServer] "+msg);
    }

    public static void chatsw(String msg){
        Log.warn("[EssentialsBanServer] "+msg);
    }
}
