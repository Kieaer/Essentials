package essential.common.database

import arc.util.Log
import essential.common.database.table.WorldHistoryTable
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object WorldHistoryBuffer {

    private data class PendingInsert(
        val time: Long,
        val player: String,
        val action: String,
        val x: Short,
        val y: Short,
        val tile: String,
        val rotate: Int,
        val team: String,
        val value: String?,
        val createdAt: Long,
    )

    private const val maxBatchSize = 100
    private const val flushIntervalMs = 200L

    private val queue = LinkedBlockingQueue<PendingInsert>()
    private val flushMutex = Mutex()
    private val stopped = AtomicBoolean(false)

    private var flushJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (flushJob != null && !flushJob!!.isCancelled) return
        stopped.set(false)
        flushJob = scope.launch(Dispatchers.IO) { flushLoop() }
    }

    fun enqueue(
        time: Long,
        player: String,
        action: String,
        x: Short,
        y: Short,
        tile: String,
        rotate: Int,
        team: String,
        value: String?,
    ) {
        if (stopped.get()) return
        queue.add(
            PendingInsert(
                time = time,
                player = player,
                action = action,
                x = x,
                y = y,
                tile = tile,
                rotate = rotate,
                team = team,
                value = value,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private fun drain(): List<PendingInsert> {
        val batch = ArrayList<PendingInsert>(maxBatchSize)
        queue.drainTo(batch)
        return batch
    }

    suspend fun flush() {
        val batch = drain()
        if (batch.isEmpty()) return
        flushBatch(batch)
    }

    suspend fun stop() {
        stopped.set(true)
        flushJob?.cancelAndJoin()
        flushJob = null
        withContext(NonCancellable) {
            val batch = drain()
            if (batch.isNotEmpty()) flushBatch(batch)
        }
    }

    private suspend fun flushLoop() {
        while (true) {
            delay(flushIntervalMs.milliseconds)
            if (stopped.get()) break
            if (queue.isEmpty()) continue
            flushMutex.withLock {
                val batch = drain()
                if (batch.isNotEmpty()) flushBatch(batch)
            }
        }
    }

    private suspend fun flushBatch(batch: List<PendingInsert>) {
        if (batch.isEmpty()) return
        runCatching {
            suspendTransaction(db = worldHistoryDatabase) {
                batch.forEach { e ->
                    WorldHistoryTable.insert { row ->
                        row[WorldHistoryTable.time] = e.time
                        row[WorldHistoryTable.player] = e.player
                        row[WorldHistoryTable.action] = e.action
                        row[WorldHistoryTable.x] = e.x
                        row[WorldHistoryTable.y] = e.y
                        row[WorldHistoryTable.tile] = e.tile
                        row[WorldHistoryTable.rotate] = e.rotate
                        row[WorldHistoryTable.team] = e.team
                        row[WorldHistoryTable.value] = e.value
                        row[WorldHistoryTable.createdAt] = Instant.fromEpochMilliseconds(e.createdAt)
                    }
                }
            }
            Log.debug("[WorldHistoryBuffer] flushed ${batch.size} entries")
        }.onFailure {
            Log.err("[WorldHistoryBuffer] flush failed", it)
        }
    }
}
