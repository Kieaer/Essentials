package essentials.external

import arc.Core
import essentials.Main.Companion.pluginRoot
import java.io.FileInputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class checksum {
    private var sum: String
    fun check(hash: String): Boolean {
        return hash == sum
    }

    fun extract() {
        pluginRoot.child("Essentials.sha512sum").writeString(sum)
    }

    init {
        val result = StringBuilder()
        try {
            var md = MessageDigest.getInstance("SHA-512")
            DigestInputStream(FileInputStream(Core.settings.dataDirectory.child("mods/Essentials.jar").absolutePath()), md).use { dis ->
                while (dis.read() != -1);
                md = dis.messageDigest
            }
            for (b in md.digest()) result.append(String.format("%02x", b))
        } catch (e: NoSuchAlgorithmException) {
            sum = "null"
        } catch (e: IOException) {
            sum = "null"
        }
        sum = result.toString()
    }
}