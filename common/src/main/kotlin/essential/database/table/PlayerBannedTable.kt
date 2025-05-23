package essential.database.table

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.json.json
import kotlinx.serialization.json.Json

object PlayerBannedTable : UIntIdTable("player_banned") {
    val names = json<List<String>>("names", Json)
    val ips = json<List<String>>("ips", Json)
    val uuid = varchar("uuid", 25)
    val reason = varchar("reason", 256)
    val date = long("date")
}