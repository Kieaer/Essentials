package essential.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/** Get current time. The Format is "yyyy-MM-dd HH:mm:ss.SSS" */
fun currentTime(): String {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).toHString()
}

fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (currentCoroutineContext().isActive) {
        emit(Unit)
        delay(period)
    }
}