package essential.util

import kotlinx.coroutines.*
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
    val hours = this.toLong(DurationUnit.SECONDS) / 3600
    val minutes = (this.toLong(DurationUnit.SECONDS) % 3600) / 60

    return when {
        hours >= 1L -> "$hours:"
        else -> ""
    } + when {
        minutes >= 1L -> " $minutes:"
        else -> ""
    }
}

fun CoroutineScope.startInfiniteScheduler(interval: Long = 1000L, task: suspend () -> Unit): Job {
    return launch {
        while (isActive) {
            task()
            delay(interval)
        }
    }
}