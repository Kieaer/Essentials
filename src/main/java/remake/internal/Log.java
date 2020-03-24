package remake.internal;

public class Log {
    public enum LogType{
        log, warn, error, debug, server, serverwarn, servererr, client, clientwarn, clienterr, config, player, playerwarn, playererr,
        tap, withdraw, block, deposit, chat, griefer, web
    }

    final static arc.util.Log.DefaultLogHandler handler = new arc.util.Log.DefaultLogHandler();

    public static void info(String value){
        String result = new Bundle().get(value);
        if(result != null){
            handler.log(arc.util.Log.LogLevel.info, result);
        } else {
            handler.log(arc.util.Log.LogLevel.info, value);
        }
    }

    public static void err(String value){
        String result = new Bundle().get(value);
        if(result != null){
            handler.log(arc.util.Log.LogLevel.err, result);
        } else {
            handler.log(arc.util.Log.LogLevel.err, value);
        }
    }

    public static void warn(String value){
        String result = new Bundle().get(value);
        if(result != null){
            handler.log(arc.util.Log.LogLevel.warn, result);
        } else {
            handler.log(arc.util.Log.LogLevel.warn, value);
        }
    }

    public static void write(String value){

    }
}
