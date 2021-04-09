package essentials.form

import java.util.*

object Garbage {
    fun <T> EqualsIgnoreCase(enumClass: Array<T>, name: String, fallback: T?): T where T : Enum<T>? {
        return Arrays.stream(enumClass).filter { e -> e?.name.equals(name, true) }.findAny().orElse(fallback)
    }
}