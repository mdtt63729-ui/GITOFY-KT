package com.gitofy.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int = AuthConfig.DEFAULT_EXPIRES_IN,
    val interval: Int = AuthConfig.DEFAULT_INTERVAL,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val interval: Int? = null,
)

sealed interface DeviceFlowEvent {
    data class Waiting(val interval: Int) : DeviceFlowEvent
    data object SlowDown : DeviceFlowEvent
    data class Approved(val token: String) : DeviceFlowEvent
    data class Failed(val error: String) : DeviceFlowEvent
}

/**
 * GitHub Device Flow — the primary login. Talks to github.com (not the API host),
 * so it uses its own OkHttp client and the public client id only; no secret.
 * Mirrors src/auth/deviceFlow.ts (pending / slow_down / backoff / resume).
 */
@Singleton
class DeviceFlowClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    suspend fun start(): DeviceCodeResponse = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", AuthConfig.CLIENT_ID)
            .add("scope", "repo read:user gist workflow")
            .build()
        val req = Request.Builder()
            .url(AuthConfig.DEVICE_CODE_URL)
            .header("Accept", "application/json")
            .post(body)
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            json.decodeFromString(DeviceCodeResponse.serializer(), text)
        }
    }

    /** Polls until approved, denied or expired, emitting progress events. */
    fun poll(device: DeviceCodeResponse): Flow<DeviceFlowEvent> = flow {
        var interval = device.interval.coerceAtLeast(1)
        val deadline = System.currentTimeMillis() + device.expiresIn * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000L)
            emit(DeviceFlowEvent.Waiting(interval))
            val tr = requestToken(device.deviceCode)
            when {
                tr?.accessToken != null -> { emit(DeviceFlowEvent.Approved(tr.accessToken)); return@flow }
                tr?.error == "authorization_pending" -> Unit
                tr?.error == "slow_down" -> { interval += 5; emit(DeviceFlowEvent.SlowDown) }
                tr?.error == "expired_token" -> { emit(DeviceFlowEvent.Failed("expired_token")); return@flow }
                tr?.error == "access_denied" -> { emit(DeviceFlowEvent.Failed("access_denied")); return@flow }
                tr?.error != null -> { emit(DeviceFlowEvent.Failed(tr.error)); return@flow }
            }
        }
        emit(DeviceFlowEvent.Failed("expired_token"))
    }

    private suspend fun requestToken(deviceCode: String): TokenResponse? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", AuthConfig.CLIENT_ID)
            .add("device_code", deviceCode)
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .build()
        val req = Request.Builder()
            .url(AuthConfig.ACCESS_TOKEN_URL)
            .header("Accept", "application/json")
            .post(body)
            .build()
        runCatching {
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                json.decodeFromString(TokenResponse.serializer(), text)
            }
        }.getOrNull()
    }
}
