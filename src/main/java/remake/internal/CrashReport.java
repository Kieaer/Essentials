package remake.internal;

import mindustry.core.Version;
import org.hjson.JsonValue;
import org.hjson.Stringify;
import remake.core.plugin.Config;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static essentials.Global.plugin_version;
import static mindustry.Vars.mods;
import static remake.Main.root;

public class CrashReport {
    Config config = new Config();

    public CrashReport(Throwable e) {
        if (!config.debug) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.toString()).append("\n");
            StackTraceElement[] element = e.getStackTrace();
            for (StackTraceElement error : element) sb.append("\tat ").append(error.toString()).append("\n");
            sb.append("=================================================\n");
            String text = sb.toString();

            //writeLog(Global.LogType.error, text);
            Log.info("Plugin internal error! - " + e.getMessage());
            if (config.crashreport) {
                try {
                    InetAddress address = InetAddress.getByName("mindustry.kr");
                    Socket socket = new Socket(address, 6560);
                    BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                    os.writeBytes(e.toString() + "\n");

                    sb = new StringBuilder();
                    sb.append(e.toString()).append("\n");
                    for (StackTraceElement error : element) sb.append("at ").append(error.toString()).append("\n");

                    StringBuilder plugins = new StringBuilder();
                    for (int a = 0; a < mods.list().size; a++) plugins.append(mods.list().get(a).name).append(", ");

                    String logs = "플러그인 버전: " + plugin_version + "\n" +
                            "서버 버전: " + Version.build + "\n" +
                            "OS: " + System.getProperty("os.name") + "\n" +
                            "플러그인 목록: " + plugins.toString().substring(0, plugins.length() - 2) + "\n" +
                            "== 설정파일 ==\n" + JsonValue.readHjson(root.child("config.hjson").readString()).toString(Stringify.HJSON) + "\n" +
                            "== Stacktrace ==\n" + sb.toString() + "\n!exit!\n";

                    os.write(logs.getBytes(StandardCharsets.UTF_8));

                    String data = is.readLine();
                    if (data != null) {
                        Log.info("Error reported!");
                    } else {
                        Log.err("Data send failed!");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            e.printStackTrace();
        }
    }
}
