package essentials.internal;

import mindustry.Vars;
import mindustry.core.Version;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static essentials.Main.*;

public class CrashReport {
    Throwable e;
    String data;
    boolean slight;
    public boolean success = false;

    public CrashReport(Throwable e, boolean... slight) {
        this.e = e;
        this.data = null;
        this.slight = slight.length != 0;
        send();
    }

    public CrashReport(Throwable e, String data, boolean... slight) {
        this.e = e;
        this.data = data;
        this.slight = slight.length != 0;
        send();
    }

    public void send() {
        Socket socket = null;
        try {
            if (!config.debug()) {
                StringBuilder sb = new StringBuilder();
                sb.append(e.toString()).append("\n");
                StackTraceElement[] element = e.getStackTrace();
                for (StackTraceElement error : element) sb.append("\tat ").append(error.toString()).append("\n");
                sb.append("=================================================\n");
                String text = sb.toString();

                Log.write(Log.LogType.error, text);
                if (!slight) Log.err("Plugin internal error! - " + e.getMessage());
                if (config.crashReport()) {

                    InetAddress address = InetAddress.getByName("mindustry.kr");
                    socket = new Socket(address, 6560);
                    try (BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                         DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {
                        os.writeBytes(e.toString() + "\n");

                        sb = new StringBuilder();
                        sb.append(e.toString()).append("\n");
                        for (StackTraceElement error : element) sb.append("at ").append(error.toString()).append("\n");

                        StringBuilder plugins = new StringBuilder();
                        for (int a = 0; a < Vars.mods.list().size; a++)
                            plugins.append(Vars.mods.list().get(a).name).append(", ");

                        String logs = "플러그인 버전: " + vars.pluginVersion() + "\n" +
                                "서버 버전: " + Version.build + "." + Version.revision + " " + Version.modifier + "\n" +
                                "OS: " + System.getProperty("os.name") + "\n" +
                                "플러그인 목록: " + (plugins.toString().contains(", ") ? plugins.toString().substring(0, plugins.length() - 2) : plugins.toString()) + "\n" +
                                "== 설정파일 ==\n" + JsonValue.readHjson(root.child("config.hjson").readString()).toString(Stringify.HJSON) + "\n" +
                                "== Stacktrace ==\n" + sb.toString() + "\n!exit!\n";

                        os.write(logs.getBytes(StandardCharsets.UTF_8));

                        String data = is.readLine();
                        if (data != null) {
                            Log.info("Error reported!");
                            success = true;
                        } else {
                            Log.err("Data send failed!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
            success = true;
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
