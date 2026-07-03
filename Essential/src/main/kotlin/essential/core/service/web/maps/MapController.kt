package essential.core.service.web.maps

import arc.Core
import arc.files.Fi
import arc.util.Log
import essential.common.database.data.getMapRatings
import essential.common.log.LogType
import essential.common.log.writeLog
import essential.common.rootPath
import essential.core.service.web.WebService.Companion.conf
import essential.core.service.web.auth.UserSession
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mindustry.Vars
import mindustry.io.MapIO
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MapInfo(
    val name: String,
    val author: String,
    val description: String,
    val planet: String,
    val preview: String? = null,
    val thumbnail: String? = null,
    val votes: Int = 0,
    val uploader: String? = null
)

class MapController {
    private class FetchTask(
        val hash: String,
        val msavBytes: ByteArray,
        val fileName: String,
        val mapName: String,
        val width: Int?,
        val deferred: CompletableDeferred<ByteArray?>
    )

    private class MapHashCacheEntry(val lastModified: Long, val size: Long, val hash: String)

    private val activeTasks = ConcurrentHashMap<String, CompletableDeferred<ByteArray?>>()
    private val fetchChannel = Channel<FetchTask>(Channel.UNLIMITED)
    private val fetchSemaphore = Semaphore(3)
    private val mapHashCache = ConcurrentHashMap<String, MapHashCacheEntry>()
    private val uploadersMap = Collections.synchronizedMap(mutableMapOf<String, String>())

    val webCacheDir = File(rootPath.child("data/webCache").absolutePath())
    val uploadersFile = File(rootPath.child("data/map_uploaders.json").absolutePath())

    fun init(scope: CoroutineScope) {
        // Create upload directory if it doesn't exist
        val uploadDir = File(conf.uploadPath)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        // Create web image cache directory if it doesn't exist
        if (!webCacheDir.exists()) {
            webCacheDir.mkdirs()
        }

        // Load map uploaders JSON if it exists
        if (uploadersFile.exists()) {
            try {
                val jsonText = uploadersFile.readText()
                val loadedMap: Map<String, String> = Json.decodeFromString(jsonText)
                uploadersMap.putAll(loadedMap)
                Log.info("Loaded ${loadedMap.size} map uploaders from JSON")
            } catch (e: Exception) {
                Log.err("Failed to load map uploaders JSON", e)
            }
        }

        scope.launch {
            processFetchQueue()
        }
        scope.launch {
            delay(5000)
            warmupMapImageCache()
        }
    }

    private fun getMapHash(mapFile: File): String? {
        if (!mapFile.exists()) return null
        val path = mapFile.absolutePath
        val lastModified = mapFile.lastModified()
        val size = mapFile.length()
        val entry = mapHashCache[path]
        if (entry != null && entry.lastModified == lastModified && entry.size == size) {
            return entry.hash
        }
        try {
            val bytes = mapFile.readBytes()
            val hash = sha256Hex(bytes)
            mapHashCache[path] = MapHashCacheEntry(lastModified, size, hash)
            return hash
        } catch (e: Exception) {
            Log.err("Failed to calculate map hash for ${mapFile.name}", e)
            return null
        }
    }

    suspend fun handleMapUpload(call: ApplicationCall) {
        val multipart = call.receiveMultipart()
        var fileName = ""
        var fileBytes: ByteArray? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    fileName = part.originalFileName ?: "unknown.msav"
                    fileBytes = part.provider().readRemaining().readByteArray()
                }
                else -> {}
            }
            part.dispose()
        }

        val bytes = fileBytes
        if (bytes == null) {
            call.respond(HttpStatusCode.BadRequest, "No file uploaded")
            return
        }

        // Check file size
        if (bytes.size > conf.maxFileSize) {
            call.respond(HttpStatusCode.BadRequest, "File too large")
            return
        }

        // Validate file extension
        if (!fileName.endsWith(".msav")) {
            call.respond(HttpStatusCode.BadRequest, "Invalid file type. Only .msav files are allowed")
            return
        }

        // Create temporary file for validation
        val tempFile = File.createTempFile("map_", ".msav")
        tempFile.writeBytes(bytes)

        // Validate map file without affecting the current game state
        try {
            val parsedMap = MapIO.createMap(Fi(tempFile.absolutePath), true)
            if (parsedMap.width <= 0 || parsedMap.height <= 0) {
                throw IllegalArgumentException("Invalid map dimensions: ${parsedMap.width}x${parsedMap.height}")
            }

            // Save the file
            val targetFile = File(conf.uploadPath, fileName)
            Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            tempFile.delete()

            val session = call.sessions.get<UserSession>()
            val username = session?.username ?: "unknown"
            val mapName = parsedMap.name()

            // Fetch and cache map image immediately on upload using the local map render API (async queueing)
            essential.core.Main.scope.launch {
                try {
                    val hash = sha256Hex(bytes)
                    queueFetchMapImage(hash, bytes, fileName, mapName)
                } catch (e: Exception) {
                    Log.err("Error pre-generating map image on upload", e)
                }
            }

            // Save uploader to JSON file
            try {
                uploadersMap[mapName] = username
                withContext(Dispatchers.IO) {
                    synchronized(uploadersMap) {
                        val jsonText = Json.encodeToString(uploadersMap.toMap())
                        uploadersFile.writeText(jsonText)
                    }
                }
            } catch (e: Exception) {
                Log.err("Failed to save map uploader to JSON file", e)
            }

            // Log the upload details
            try {
                writeLog(
                    LogType.Web,
                    "User '$username' uploaded file '$fileName' (Map name: '${parsedMap.plainName()}', Author: '${parsedMap.plainAuthor()}', Version: ${parsedMap.version}, Build: ${parsedMap.build}, Size: ${parsedMap.width}x${parsedMap.height})"
                )
            } catch (le: Exception) {
                Log.err("Error writing upload log", le)
            }

            // Reload maps
            Core.app.post {
                Vars.maps.reload()
                Log.info("Maps reloaded after upload: $fileName")
            }

            call.respond(HttpStatusCode.OK, "Map uploaded successfully")
        } catch (e: Exception) {
            tempFile.delete()
            Log.err("Map validation error", e)
            call.respond(HttpStatusCode.BadRequest, "Invalid map file: ${e.message}")
        }
    }

    suspend fun handleMapDownload(call: ApplicationCall, mapName: String) {
        val map = Vars.maps.all().find { it.name() == mapName }
        if (map == null) {
            call.respond(HttpStatusCode.NotFound, "Map not found")
            return
        }

        val file = File(map.file.absolutePath())
        if (!file.exists()) {
            call.respond(HttpStatusCode.NotFound, "Map file not found")
            return
        }

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "${map.name()}.msav")
                .toString()
        )
        call.respondFile(file)
    }

    suspend fun handleMapImage(call: ApplicationCall, nameOrHash: String) {
        val width = call.request.queryParameters["width"]?.toIntOrNull()
        val suffix = if (width != null && width > 0) "_w$width" else ""

        val isHash = nameOrHash.length == 64 && nameOrHash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        if (isHash) {
            val cacheFile = File(webCacheDir, "${nameOrHash.lowercase()}$suffix.avif")
            if (withContext(Dispatchers.IO) { cacheFile.exists() }) {
                call.respondFile(cacheFile)
                return
            }
        }

        val map = if (isHash) {
            Vars.maps.all().find { map ->
                val hash = getMapHash(File(map.file.absolutePath()))
                hash.equals(nameOrHash, ignoreCase = true)
            }
        } else {
            Vars.maps.all().find { it.name() == nameOrHash }
        }

        if (map == null) {
            call.respond(HttpStatusCode.NotFound, "Map not found")
            return
        }

        val msavFile = File(map.file.absolutePath())
        if (!msavFile.exists()) {
            call.respond(HttpStatusCode.NotFound, "Map file not found")
            return
        }

        val msavBytes = withContext(Dispatchers.IO) { msavFile.readBytes() }
        val hash = sha256Hex(msavBytes)
        val cacheFile = File(webCacheDir, "$hash$suffix.avif")

        // Fetch and cache image if missing
        if (!withContext(Dispatchers.IO) { cacheFile.exists() }) {
            try {
                val image = queueFetchMapImage(hash, msavBytes, map.file.name(), map.name(), width)
                if (image == null) {
                    call.respond(HttpStatusCode.BadGateway, "Failed to fetch map image")
                    return
                }
            } catch (e: Exception) {
                Log.err("Failed to fetch/cache map image for '${map.name()}'", e)
                call.respond(HttpStatusCode.BadGateway, "Failed to fetch map image")
                return
            }
        }

        call.respondFile(cacheFile)
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun warmupMapImageCache() {
        try {
            val maps = Vars.maps.all().filter { it.custom }
            if (maps.isEmpty()) {
                Log.debug("Warmup: no custom maps found")
                return
            }
            Log.info("Warmup: checking ${maps.size} custom maps for missing image cache")
            var queued = 0
            for (map in maps) {
                val msavFile = File(map.file.absolutePath())
                if (!msavFile.exists()) {
                    Log.debug("Warmup: map '${map.name()}' msav file missing, skipping")
                    continue
                }
                val msavBytes = withContext(Dispatchers.IO) { msavFile.readBytes() }
                val hash = sha256Hex(msavBytes)
                val cacheFile = File(webCacheDir, "$hash.avif")
                if (!cacheFile.exists()) {
                    Log.info("Warmup: queuing image fetch for map '${map.name()}' (cache missing)")
                    queueFetchMapImage(hash, msavBytes, map.file.name(), map.name())
                    queued++
                }
            }
            Log.info("Warmup: queued $queued missing map image(s) for rendering")
        } catch (e: Exception) {
            Log.err("Warmup: failed to pre-generate missing map images", e)
        }
    }

    private suspend fun processFetchQueue() = coroutineScope {
        val pending = mutableListOf<Deferred<Boolean>>()
        var submitted = 0
        for (task in fetchChannel) {
            submitted++
            Log.debug("Fetch queue: submitted task #$submitted for map '${task.mapName}' (width: ${task.width})")
            val deferred = async(Dispatchers.IO) {
                fetchSemaphore.withPermit {
                    try {
                        Log.debug("Processing map image fetch request for map: ${task.mapName} (file: ${task.fileName}, width: ${task.width})")
                        val result = fetchMapImageWithRetry(task.msavBytes, task.fileName, task.mapName, task.width)
                        val suffix = if (task.width != null && task.width > 0) "_w${task.width}" else ""
                        if (result != null) {
                            val cacheFile = File(webCacheDir, "${task.hash}$suffix.avif")
                            val tmp = File(webCacheDir, "${task.hash}$suffix.avif.tmp")
                            tmp.writeBytes(result)
                            Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        task.deferred.complete(result)
                    } catch (e: Exception) {
                        Log.err("Error processing fetch task for map '${task.mapName}'", e)
                        task.deferred.complete(null)
                    } finally {
                        val taskKey = if (task.width != null && task.width > 0) "${task.hash}_w${task.width}" else task.hash
                        activeTasks.remove(taskKey)
                    }
                }
            }
            pending.add(deferred)
        }
        pending.awaitAll()
    }

    private suspend fun queueFetchMapImage(hash: String, msavBytes: ByteArray, fileName: String, mapName: String, width: Int? = null): ByteArray? {
        val suffix = if (width != null && width > 0) "_w$width" else ""
        val cacheFile = File(webCacheDir, "$hash$suffix.avif")
        if (cacheFile.exists()) {
            return withContext(Dispatchers.IO) {
                cacheFile.readBytes()
            }
        }

        val taskKey = if (width != null && width > 0) "${hash}_w$width" else hash

        val deferred = activeTasks.compute(taskKey) { _, existing ->
            if (existing != null) {
                existing
            } else {
                val newDeferred = CompletableDeferred<ByteArray?>()
                val task = FetchTask(hash, msavBytes, fileName, mapName, width, newDeferred)
                val sendResult = fetchChannel.trySend(task)
                if (sendResult.isFailure) {
                    newDeferred.complete(null)
                    activeTasks.remove(taskKey)
                }
                newDeferred
            }
        }!!

        return deferred.await()
    }

    private suspend fun fetchMapImageWithRetry(msavBytes: ByteArray, fileName: String, mapName: String, width: Int? = null): ByteArray? {
        var attempts = 0
        val maxAttempts = 3
        val retryDelay = 10.seconds

        while (attempts < maxAttempts) {
            attempts++
            try {
                Log.debug("Submitting render job to map render API for map '$mapName' (width: $width, attempt $attempts/$maxAttempts)")
                val result = withContext(Dispatchers.IO) {
                    fetchMapImageBatch(msavBytes, fileName, mapName, width)
                }
                if (result != null) {
                    return result
                }
                Log.warn("Render job returned null for map '$mapName' (width: $width, attempt $attempts/$maxAttempts)")
            } catch (e: Exception) {
                Log.err("Exception during batch render for map '$mapName' (width: $width): ${e.message} (attempt $attempts/$maxAttempts)")
            }
            if (attempts < maxAttempts) {
                delay(retryDelay)
            }
        }
        return null
    }

    private fun fetchMapImageBatch(msavBytes: ByteArray, fileName: String, mapName: String, width: Int? = null): ByteArray? {
        val baseUrl = "http://192.168.0.48:7000"
        val jobId = submitRenderJob(baseUrl, msavBytes, fileName, mapName, width)
            ?: return null

        Log.debug("Render job submitted for map '$mapName' (width: $width): jobId=$jobId")

        val pollIntervalMs = 2000L
        val maxWaitMs = 600_000L // 10 minutes per job
        val deadline = System.currentTimeMillis() + maxWaitMs

        while (System.currentTimeMillis() < deadline) {
            val status = pollJobStatus(baseUrl, jobId)
            if (status == null) {
                Log.err("Failed to poll job status for map '$mapName' (jobId=$jobId)")
                return null
            }
            when (status) {
                "done" -> {
                    Log.debug("Render job done for map '$mapName' (jobId=$jobId), fetching result")
                    return fetchJobResult(baseUrl, jobId)
                }
                "failed" -> {
                    Log.err("Render job failed for map '$mapName' (jobId=$jobId)")
                    return null
                }
                "queued", "rendering" -> {
                    Thread.sleep(pollIntervalMs)
                }
                else -> {
                    Log.err("Unknown job status '$status' for map '$mapName' (jobId=$jobId)")
                    return null
                }
            }
        }
        Log.err("Render job timed out for map '$mapName' (jobId=$jobId) after ${maxWaitMs / 1000}s")
        return null
    }

    private fun submitRenderJob(baseUrl: String, msavBytes: ByteArray, fileName: String, mapName: String, width: Int? = null): String? {
        val boundary = "----EssentialBoundary${System.nanoTime()}"
        val url = if (width != null && width > 0) URL("$baseUrl/jobs?width=$width") else URL("$baseUrl/jobs")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("Connection", "close")

            DataOutputStream(conn.outputStream).use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8))
                out.write("Content-Type: application/octet-stream\r\n\r\n".toByteArray())
                out.write(msavBytes)
                out.write("\r\n".toByteArray())
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"mapName\"\r\n".toByteArray())
                out.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(mapName.toByteArray(Charsets.UTF_8))
                out.write("\r\n".toByteArray())
                out.write("--$boundary--\r\n".toByteArray())
                out.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorBody = try { conn.errorStream?.use { it.readBytes().decodeToString() } } catch (_: Exception) { null }
                Log.err("Failed to submit render job for map '$mapName' (HTTP $responseCode): $errorBody")
                return null
            }

            val body = conn.inputStream.use { it.readBytes().decodeToString() }
            val json = Json.parseToJsonElement(body).jsonObject
            return json["jobId"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.err("Exception submitting render job for map '$mapName': ${e.message}")
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun pollJobStatus(baseUrl: String, jobId: String): String? {
        val url = URL("$baseUrl/jobs/$jobId")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Connection", "close")

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                return null
            }
            val body = conn.inputStream.use { it.readBytes().decodeToString() }
            val json = Json.parseToJsonElement(body).jsonObject
            return json["status"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchJobResult(baseUrl: String, jobId: String): ByteArray? {
        val url = URL("$baseUrl/jobs/$jobId/result")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            conn.setRequestProperty("Connection", "close")

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorBody = try { conn.errorStream?.use { it.readBytes().decodeToString() } } catch (_: Exception) { null }
                Log.err("Failed to fetch render result (jobId=$jobId, HTTP $responseCode): $errorBody")
                return null
            }
            return conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            Log.err("Exception fetching render result (jobId=$jobId): ${e.message}")
            return null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun getMaps(): List<MapInfo> {
        val mapsList = mutableListOf<MapInfo>()
        Vars.maps.all().filter { it.custom }.forEach { map ->
            val mapName = map.name()
            val ratings = getMapRatings(mapName)
            val upvotes = ratings.count { it.rating >= 3 }
            val downvotes = ratings.count { it.rating < 3 }
            val netVotes = upvotes - downvotes
            val uploader = uploadersMap[mapName]

            val hash = getMapHash(File(map.file.absolutePath()))
            val previewUrl = hash ?: URLEncoder.encode(mapName, "UTF-8")

            mapsList.add(
                MapInfo(
                    name = mapName,
                    author = map.author(),
                    description = map.description(),
                    planet = map.tags.get("planet", "serpulo"),
                    preview = "api/maps/image/$previewUrl",
                    thumbnail = "api/maps/image/$previewUrl?width=480",
                    votes = netVotes,
                    uploader = uploader
                )
            )
        }
        return mapsList
    }
}

fun Route.mapRoutes(controller: MapController) {
    route("/api/maps") {
        authenticate("auth-session") {
            get {
                val maps = controller.getMaps()
                call.respond(maps)
            }

            post("/upload") {
                controller.handleMapUpload(call)
            }

            get("/download/{name}") {
                val mapName = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                controller.handleMapDownload(call, mapName)
            }

            get("/image/{name}") {
                val nameOrHash = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                controller.handleMapImage(call, nameOrHash)
            }
        }
    }
}
