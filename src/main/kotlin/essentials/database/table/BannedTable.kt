package essentials.database.table

import org.jetbrains.exposed.sql.Table

object BannedTable: Table() {
    val type = enumeration<BannedType>("type")
    val data = text("data")
}

enum class BannedType {
    UUID, IP
}