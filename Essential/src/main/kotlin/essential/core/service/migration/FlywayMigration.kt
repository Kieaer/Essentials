package essential.core.service.migration

import arc.util.Log
import essential.common.bundle
import essential.common.database.parseR2dbcUrl
import essential.core.Main
import org.flywaydb.core.Flyway

/** Flyway-backed migration entry point loaded reflectively by Database.kt. */
object FlywayMigration {
    @JvmStatic
    fun migrate(databaseType: String, r2dbcUrl: String, user: String, pass: String): String? {
        val (jdbcUrl, effectiveUser, effectivePass) = when (databaseType) {
            "postgresql" -> {
                val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "postgresql://", "5432")
                Triple("jdbc:postgresql://$host:$port/$database", user, pass)
            }

            "mysql" -> {
                val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "mysql://", "3306")
                Triple("jdbc:mysql://$host:$port/$database", user, pass)
            }

            "mariadb" -> {
                val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "mariadb://", "3306")
                Triple("jdbc:mariadb://$host:$port/$database", user, pass)
            }

            else -> Triple("jdbc:h2:./config/mods/Essentials/data/database;AUTO_SERVER=TRUE", "sa", "123")
        }

        val modClassLoader = Main::class.java.classLoader
        val previousClassLoader = Thread.currentThread().contextClassLoader
        return try {
            Thread.currentThread().contextClassLoader = modClassLoader
            val flyway = Flyway.configure(modClassLoader)
                .dataSource(jdbcUrl, effectiveUser, effectivePass)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("5")
                .load()
            flyway.migrate()
            val currentVersion = flyway.info().current()?.version?.version ?: "5"
            Log.info(bundle["database.upgrade.upToDate", currentVersion])
            currentVersion
        } catch (e: Exception) {
            Log.err("Flyway migration failed: ${e.message}", e)
            null
        } finally {
            Thread.currentThread().contextClassLoader = previousClassLoader
        }
    }
}
