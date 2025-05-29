package essential.database.table

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table

object PlayerBannedTable : Table("player_banned") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val names = json<List<String>>("names", Json)
    val ips = json<List<String>>("ips", Json)
    val uuid = varchar("uuid", 25)
    val reason = varchar("reason", 256)
    val date = long("date")

    override val primaryKey = PrimaryKey(id)
}