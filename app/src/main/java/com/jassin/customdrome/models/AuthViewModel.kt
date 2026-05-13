package com.jassin.customdrome.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Serializable
private data class LoginPayload(
    val username: String,
    val password: String,
)

class AuthViewModel : ViewModel() {
    private val client =
        HttpClient(OkHttp) {
            engine {
                config {
                    // 1. Create a trust manager that does not validate certificate chains
                    val trustAllCerts =
                        object : X509TrustManager {
                            override fun checkClientTrusted(
                                p0: Array<X509Certificate>,
                                p1: String,
                            ) {}

                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
                            ) {}

                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }

                    // 2. Install the all-trusting trust manager
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

                    sslSocketFactory(sslContext.socketFactory, trustAllCerts)

                    // 3. Ignore hostname verification (matches IP/domain to cert)
                    hostnameVerifier { _, _ -> true }
                }
            }

            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result

    private val _request = MutableStateFlow<String?>(null)
    val request: StateFlow<String?> = _request

    fun login(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
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

                _result.value = "HTTP ${response.status.value}\n${response.bodyAsText()}"
            } catch (t: Throwable) {
                _result.value = "Request failed: ${t::class.simpleName}: ${t.message}"
            }
        }
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }
}
