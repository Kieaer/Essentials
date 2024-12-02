package essential.core

import java.sql.*
import java.util.*
import java.util.logging.Logger

class DriverLoader(private val driver: Driver) : Driver {
    @Throws(SQLException::class)
    override fun acceptsURL(u: String): Boolean {
        return driver.acceptsURL(u)
    }

    @Throws(SQLException::class)
    override fun connect(u: String, p: Properties): Connection {
        return driver.connect(u, p)
    }

    override fun getMajorVersion(): Int {
        return driver.majorVersion
    }

    override fun getMinorVersion(): Int {
        return driver.minorVersion
    }

    @Throws(SQLException::class)
    override fun getPropertyInfo(u: String, p: Properties): Array<DriverPropertyInfo> {
        return driver.getPropertyInfo(u, p)
    }

    override fun jdbcCompliant(): Boolean {
        return driver.jdbcCompliant()
    }

    @Throws(SQLFeatureNotSupportedException::class)
    override fun getParentLogger(): Logger {
        return driver.parentLogger
    }
}