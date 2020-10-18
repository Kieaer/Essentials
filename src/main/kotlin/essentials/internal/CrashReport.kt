package essentials.internal

import essentials.Config
import essentials.Main.Companion.pluginRoot
import essentials.PluginVars
import mindustry.Vars
import mindustry.core.Version
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.*
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class CrashReport {
    var e: Throwable
    var data: String?
    var success = false

    constructor(e: Throwable) {
        this.e = e
        data = null
        send()
    }

    constructor(e: Throwable, data: String?) {
        this.e = e
        this.data = data
        send()
    }

    private fun send() {
        var socket: Socket? = null
        try {
            if (!Config.debug) {
                var sb = StringBuilder()
                sb.append(e.toString()).append("\n")
                val element = e.stackTrace
                for (error in element) sb.append("\tat ").append(error.toString()).append("\n")
                sb.append("=================================================\n")
                val text = sb.toString()
                Log.write(Log.LogType.Error, text)
                Log.err("Plugin internal error! - " + e.message)
                Log.err(text)
                if (Config.crashReport) {
                    val address = InetAddress.getByName("mindustry.kr")
                    socket = Socket(address, 6560)
                    BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { `is` ->
                        DataOutputStream(socket.getOutputStream()).use { os ->
                            os.writeBytes("""$e""".trimIndent())
                            sb = StringBuilder()
                            sb.append(e.toString()).append("\n")
                            for (error in element) sb.append("at ").append(error.toString()).append("\n")
                            val plugins = StringBuilder()
                            for (a in 0 until Vars.mods.list().size) plugins.append(Vars.mods.list()[a].name).append(", ")
                            val logs = "플러그인 버전: ${PluginVars.pluginVersion}\n" + "" +
                                    "서버 버전: ${Version.build}.${Version.revision} ${Version.modifier}\n" +
                                    "OS: ${System.getProperty("os.name")}\n" +
                                    "플러그인 목록: ${if (plugins.toString().contains(", ")) plugins.toString().substring(0, plugins.length - 2) else plugins.toString()}\n" +
                                    "== 설정파일 ==\n" +
                                    "${JsonValue.readHjson(pluginRoot.child("config.hjson").readString()).toString(Stringify.HJSON)}\n" +
                                    "== Stacktrace ==\n" +
                                    "$sb\n" +
                                    "!exit!"
                            os.write(logs.toByteArray(StandardCharsets.UTF_8))
                            val data = `is`.readLine()
                            if (data != null) {
                                Log.info("Error reported!")
                                success = true
                            } else {
                                Log.err("Data send failed!")
                            }
                        }
                    }
                }
            }
        } catch (e: ConnectException){
            Log.warn("remote-server-dead")
            success = true
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            println(sw.toString())
            success = true
        } finally {
            try {
                socket?.close()
            } catch (ignored: IOException) {
            }
        }
    }

    fun print(): String {
        val sb = StringBuilder()
        sb.append(e.toString()).append("\n")
        for (error in e.stackTrace) sb.append("at ").append(error.toString()).append("\n")
        return sb.toString()
    }
}