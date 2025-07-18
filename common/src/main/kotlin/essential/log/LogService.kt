package essential.log

import arc.Core
import essential.rootPath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

private val logFiles = HashMap<LogType, FileAppender>()

fun init() {
    for (type in LogType.entries) {
        logFiles[type] = FileAppender(rootPath.child("log/$type.log").file())
    }
}

fun writeLog(type: LogType, text: String, vararg name: String) {
    val maxLogFile = 20
    val time = DateTimeFormatter.ofPattern("YYYY-MM-dd HH_mm_ss").format(LocalDateTime.now())

    if (!rootPath.child("log/old/$type").exists()) {
        rootPath.child("log/old/$type").mkdirs()
    }
    if (!rootPath.child("log/$type.log").exists()) {
        rootPath.child("log/$type.log").writeString("")
    }

    if (type != LogType.Report) {
        val new = Paths.get(rootPath.child("log/$type.log").path())
        val old = Paths.get(rootPath.child("log/old/$type/$time.log").path())
        var main = logFiles[type]
        val folder = rootPath.child("log")

        if (main != null && main.length() > 2048 * 1024) {
            main.write("end of file. $time")
            main.close()
            try {
                if (!rootPath.child("log/old/$type").exists()) {
                    rootPath.child("log/old/$type").mkdirs()
                }
                Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                val logFiles =
                    rootPath.child("log/old/$type").file().listFiles { file -> file.name.endsWith(".log") }

                if (logFiles != null && logFiles.size >= maxLogFile) {
                    val zipFileName = "$time.zip"
                    val zipOutputStream = ZipOutputStream(FileOutputStream(zipFileName))

                    Thread {
                        for (logFile in logFiles) {
                            val entryName = logFile.name
                            val zipEntry = ZipEntry(entryName)
                            zipOutputStream.putNextEntry(zipEntry)

                            val fileInputStream = FileInputStream(logFile)
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (fileInputStream.read(buffer).also { length = it } > 0) {
                                zipOutputStream.write(buffer, 0, length)
                            }

                            fileInputStream.close()
                            zipOutputStream.closeEntry()
                        }

                        zipOutputStream.close()

                        logFiles.forEach {
                            it.delete()
                        }

                        Files.move(
                            Path(Core.files.external(zipFileName).absolutePath()),
                            Path(rootPath.child("log/old/$type/$zipFileName").absolutePath())
                        )
                    }.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            main = null
        }
        if (main == null) {
            logFiles[type] = FileAppender(folder.child("$type.log").file())
        } else {
            main.write("[$time] $text")
        }
    } else {
        val main = rootPath.child("log/report/$time-${name[0]}.txt")
        main.writeString(text)
    }
}

class FileAppender(private val file: File) {
    private val raf: RandomAccessFile

    init {
        if (!file.exists()) {
            file.writeText("")
        }
        raf = RandomAccessFile(file, "rw")
    }

    fun write(text: String) {
        raf.write(("\n$text").toByteArray(StandardCharsets.UTF_8))
    }

    fun length(): Long {
        return file.length()
    }

    fun close() {
        raf.close()
    }
}