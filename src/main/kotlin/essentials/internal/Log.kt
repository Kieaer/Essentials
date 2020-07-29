package essentials.internal

import arc.util.Log
import essentials.Main
import essentials.Main.Companion.pluginRoot
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Log {
    fun info(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.info(MessageFormat.format(result, *parameter))
    }

    fun err(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.err(MessageFormat.format(result, *parameter))
    }

    fun warn(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.warn(MessageFormat.format(result, *parameter))
    }

    fun server(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.info("[EssentialServer] " + MessageFormat.format(result, *parameter))
    }

    fun client(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.info("[EssentialClient] " + MessageFormat.format(result, *parameter))
    }

    fun player(value: String, vararg parameter: Any?) {
        val result = Bundle()[value]
        Log.info("[EssentialPlayer] " + MessageFormat.format(result, *parameter))
    }

    fun write(type: LogType, value: String, vararg params: Any?) {
        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now())
        val newlog = Paths.get(pluginRoot.child("log/$type.log").path())
        val oldlog = Paths.get(pluginRoot.child("log/old/$type/$date.log").path())
        var mainlog = pluginRoot.child("log/$type.log")
        val logfolder = pluginRoot.child("log")
        if (mainlog != null && mainlog.length() > 1024 * 256) {
            mainlog.writeString(Bundle().get("log.file-end", date), true)
            try {
                Files.move(newlog, oldlog, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                CrashReport(e)
            }
            mainlog = null
        }
        if (mainlog == null) mainlog = logfolder.child("$type.log")
        mainlog!!.writeString("""[${Main.tool.getLocalTime()}] ${Main.configs.bundle[value, params]}""".trimIndent(), true)
    }

    enum class LogType {
        log, warn, error, debug, server, serverwarn, servererr, client, clientwarn, clienterr, config, player, playerwarn, playererr, tap, withdraw, block, deposit, chat, griefer, web
    }
}