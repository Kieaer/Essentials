package essential.core.service.web.auth

import arc.util.Log
import essential.common.database.data.getPlayerDataByName
import essential.core.service.web.WebService.Companion.conf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt

@Serializable
data class UserSession(val id: String, val username: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

class AuthController {
    suspend fun handleLogin(call: ApplicationCall, request: LoginRequest) {
        try {
            val playerData = getPlayerDataByName(request.username)

            if (playerData == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                return
            }

            // Check if account ID and password are set
            if (playerData.accountID == null || playerData.accountPW == null) {
                call.respond(HttpStatusCode.Unauthorized, "Account not set up")
                return
            }

            // Check if account ID and password are the same
            if (playerData.accountID == request.password) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Your username and password are the same. Please change your password."
                )
                return
            }

            // Check if Discord ID is set
            if (playerData.discordID == null) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("message" to "Please link your Discord account first", "discordUrl" to conf.discordUrl)
                )
                return
            }

            // Verify password using BCrypt
            val passwordMatches = suspendTransaction {
                val storedHash = playerData.accountPW
                if (storedHash != null) {
                    BCrypt.checkpw(request.password, storedHash)
                } else {
                    false
                }
            }

            if (!passwordMatches) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                return
            }

            // Create session
            val session = UserSession(playerData.id.toString(), playerData.name)
            call.sessions.set(session)

            call.respond(HttpStatusCode.OK, mapOf("username" to playerData.name))
        } catch (e: Exception) {
            Log.err("Login error", e)
            call.respond(HttpStatusCode.InternalServerError, "An error occurred during login")
        }
    }
}

fun Route.authRoutes(controller: AuthController) {
    route("/api/auth") {
        post("/login") {
            val loginRequest = call.receive<LoginRequest>()
            controller.handleLogin(call, loginRequest)
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respond(HttpStatusCode.OK)
        }

        get("/status") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respond(mapOf("username" to session.username))
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
