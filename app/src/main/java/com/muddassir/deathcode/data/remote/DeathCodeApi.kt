package com.muddassir.deathcode.data.remote

import com.muddassir.deathcode.data.remote.dto.AuthResponse
import com.muddassir.deathcode.data.remote.dto.CommunitySubmissionDto
import com.muddassir.deathcode.data.remote.dto.CommunitySubmissionResponse
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto
import com.muddassir.deathcode.data.remote.dto.LoginRequest
import com.muddassir.deathcode.data.remote.dto.ManifestDto
import com.muddassir.deathcode.data.remote.dto.SignupRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Raised for any non-successful API interaction. */
class ApiException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isNetworkError: Boolean get() = statusCode == 0
}

/**
 * Minimal HTTPS client for the Death Code backend.
 *
 * Uses `HttpURLConnection` deliberately: the app only talks to a handful of JSON
 * endpoints, so a full HTTP stack would be dead weight. The backend lives behind HTTPS on
 * Render — the Android app never sees the database.
 */
class DeathCodeApi(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    },
) {

    suspend fun fetchManifest(baseUrl: String, kind: String): ManifestDto =
        get(baseUrl, "/api/content/$kind/manifest", token = null)

    suspend fun fetchPackage(baseUrl: String, kind: String): ContentPackageDto =
        get(baseUrl, "/api/content/$kind/package", token = null)

    suspend fun signup(baseUrl: String, email: String, password: String, displayName: String?): AuthResponse =
        post(baseUrl, "/api/auth/signup", SignupRequest(email, password, displayName), token = null)

    suspend fun login(baseUrl: String, email: String, password: String): AuthResponse =
        post(baseUrl, "/api/auth/login", LoginRequest(email, password), token = null)

    suspend fun submitCommunity(
        baseUrl: String,
        token: String,
        submission: CommunitySubmissionDto,
    ): CommunitySubmissionResponse =
        post(baseUrl, "/api/community/submissions", submission, token)

    // --------------------------------------------------------------- plumbing

    private suspend inline fun <reified T> get(baseUrl: String, path: String, token: String?): T =
        json.decodeFromString(request("GET", baseUrl, path, token, body = null))

    private suspend inline fun <reified B, reified T> post(
        baseUrl: String,
        path: String,
        body: B,
        token: String?,
    ): T = json.decodeFromString(
        request("POST", baseUrl, path, token, body = json.encodeToString(body)),
    )

    private suspend fun request(
        method: String,
        baseUrl: String,
        path: String,
        token: String?,
        body: String?,
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            throw ApiException(0, "No backend configured")
        }

        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            body?.let { payload ->
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let(::readAll) ?: ""

            if (status !in 200..299) {
                val message = runCatching {
                    json.decodeFromString<com.muddassir.deathcode.data.remote.dto.ApiErrorDto>(text).error
                }.getOrNull() ?: "Request failed with status $status"
                throw ApiException(status, message)
            }
            text
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(0, e.message ?: "Network error", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun readAll(stream: java.io.InputStream): String =
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
