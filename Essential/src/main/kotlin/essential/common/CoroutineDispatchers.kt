package essential.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Centralized dispatcher provider used across the app.
 * In production, defaults to real dispatchers; in tests, override these to
 * use kotlinx-coroutines-test dispatchers for deterministic execution.
 */
object AppDispatchers {
    @Volatile
    var io: CoroutineDispatcher = Dispatchers.IO
}
