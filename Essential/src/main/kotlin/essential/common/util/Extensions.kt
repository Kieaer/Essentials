package essential.common.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** Convert to human-readable string */
fun LocalDateTime.toHString(): String {
    return this.format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        hour(); char(':'); minute()
        optional {
            char(':'); second()
            optional {
                char('.'); secondFraction(maxLength = 3)
            }
        }
    })
}

/** Convert to human-readable string */
fun Duration.toHString(): String {
    val totalSeconds = this.toLong(DurationUnit.SECONDS)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val hp = if (hours > 0) "${hours.toString().padStart(2, '0')}:" else ""
    val mp = minutes.toString().padStart(2, '0')
    val sp = seconds.toString().padStart(2, '0')
    return "$hp$mp:$sp"
}

/** Extension property to allow Groups.build.size and Groups.unit.size */
val <T : mindustry.gen.Entityc> mindustry.entities.EntityGroup<T>.size: Int
    get() = this.size()