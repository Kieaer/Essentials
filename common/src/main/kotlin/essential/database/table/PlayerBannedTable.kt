package essential.database.table

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.json.json

object PlayerBannedTable : UIntIdTable("player_banned") {
    val names = json<List<String>>("names")
    val ips = json<List<String>>("ips")
    val uuid = varchar("uuid", 24)
    val reason = varchar("reason", 256)
    val date = long("date")
}