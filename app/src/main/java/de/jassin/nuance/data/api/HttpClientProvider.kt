package de.jassin.nuance.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object HttpClientProvider {
    @Suppress("unused")
    fun create(secureHostnameChecking: Boolean = true): HttpClient =
        if (!secureHostnameChecking) {
            HttpClient(OkHttp) {
                engine {
                    config {
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

                        val sslContext = SSLContext.getInstance("SSL")
                        sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

                        sslSocketFactory(sslContext.socketFactory, trustAllCerts)
                        hostnameVerifier { _, _ -> true }
                    }
                }

                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        } else {
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }
}
