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
    // singleton http client so that we use the same settings everywhere
    // ignore ssl errors and allow http
    val client: HttpClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    // don't verify certificates
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

                    // all-trusting trust manager
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

                    sslSocketFactory(sslContext.socketFactory, trustAllCerts)

                    // ignore hostname verification
                    hostnameVerifier { _, _ -> true }
                }
            }

            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    fun close() {
        client.close()
    }
}
