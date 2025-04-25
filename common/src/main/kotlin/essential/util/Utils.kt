package essential.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Get current time. The Format is "yyyy-MM-dd HH:mm:ss.SSS" */
fun currentTime(): String {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).toHString()
}