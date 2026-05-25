package de.jassin.nuance.data.models

import androidx.lifecycle.ViewModel
import de.jassin.nuance.data.api.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val id: String,
    val isAdmin: Boolean,
    val subsonicToken: String,
    val subsonicSalt: String? = null,
    val token: String,
    val name: String,
)

@Serializable
private data class LoginPayload(
    val username: String,
    val password: String,
)

class AuthViewModel : ViewModel() {
    private val client = HttpClientProvider.client

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result

    private val _request = MutableStateFlow<String?>(null)
    val request: StateFlow<String?> = _request

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): LoginResponse? {
        try {
            val base = serverUrl.trimEnd('/')
            val url = "$base/auth/login"

            val payload =
                LoginPayload(
                    username = username,
                    password = password,
                )

            // Show what we're about to send
            _request.value =
                buildString {
                    appendLine("POST $url")
                    appendLine("Content-Type: application/json")
                    appendLine("""Body: {"username":"$username","password":"$password"}""")
                }

            val response =
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

            if (response.status == HttpStatusCode.OK) {
                // This is the magic line. It automatically deserializes
                // the JSON body into your LoginResponse object.
                val loginData: LoginResponse = response.body()

                _result.value = "Success! Welcome ${loginData.name}. Debug: Your token is: ${loginData.token}"
                return loginData
            } else {
                _result.value = "Login failed: ${response.status}"
            }

            _result.value = "HTTP ${response.status.value} ${response.bodyAsText()}"
        } catch (t: Throwable) {
            _result.value = "Request failed: ${t::class.simpleName}: ${t.message}"
        }
        return null
    }

    override fun onCleared() {
        // NOT closing the shared HttpClient here
        // was the cause of MANY HOURS OF HEADACHES grrrr
        // LLM explanation on why:
        // The client provided by `HttpClientProvider` is a singleton for the
        // whole app. Closing it in a ViewModel causes later requests to fail
        // with "parent job is completed" / cancellation errors because the
        // underlying engine/coroutine context is shutdown.
        super.onCleared()
    }
}
