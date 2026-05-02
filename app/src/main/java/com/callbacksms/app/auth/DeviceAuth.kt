package com.callbacksms.app.auth

import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection

object DeviceAuth {

    // ★ Firebase 콘솔 → Realtime Database → URL 복사 후 입력
    //   예: "https://my-project-default-rtdb.firebaseio.com"
    //   비워두면 인증 비활성화 (개발용)
    const val FIREBASE_DB_URL = ""

    private const val PREF_NAME = "device_auth"
    private const val KEY_LICENSE = "license_key"

    fun isConfigured() = FIREBASE_DB_URL.isNotBlank()

    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    fun getStoredLicense(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LICENSE, null)

    fun saveLicense(context: Context, key: String) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LICENSE, key.uppercase().trim()).apply()

    fun clearLicense(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LICENSE).apply()

    sealed class Result {
        object Allowed : Result()
        data class Denied(val reason: String) : Result()
        object NetworkError : Result()
    }

    fun validate(context: Context, licenseKey: String, onResult: (Result) -> Unit) {
        if (!isConfigured()) { onResult(Result.Allowed); return }

        val key = licenseKey.uppercase().trim()
        val deviceId = getDeviceId(context)

        Thread {
            val result = try {
                // 1. 라이선스 정보 조회
                val licenseJson = httpGet("$FIREBASE_DB_URL/licenses/$key.json")
                    ?: return@Thread onResult(
                        android.os.Handler(android.os.Looper.getMainLooper()).let {
                            it.post { onResult(Result.Denied("유효하지 않은 코드입니다")) }
                            return@Thread
                        }
                    )

                if (licenseJson == "null") {
                    Result.Denied("유효하지 않은 코드입니다")
                } else {
                    val json = JSONObject(licenseJson)
                    val active = json.optBoolean("active", false)
                    val maxDevices = json.optInt("maxDevices", 1)
                    val devicesObj = json.optJSONObject("devices")
                    val deviceCount = devicesObj?.length() ?: 0
                    val alreadyRegistered = devicesObj?.has(deviceId) == true

                    when {
                        !active ->
                            Result.Denied("사용이 중지된 코드입니다")
                        !alreadyRegistered && deviceCount >= maxDevices ->
                            Result.Denied("이 코드는 최대 ${maxDevices}대까지 사용 가능합니다\n(현재 ${deviceCount}대 등록됨)")
                        else -> {
                            // 2. 이 기기가 아직 미등록이면 Firebase에 등록
                            if (!alreadyRegistered) {
                                httpPut(
                                    "$FIREBASE_DB_URL/licenses/$key/devices/$deviceId.json",
                                    "true"
                                )
                            }
                            Result.Allowed
                        }
                    }
                }
            } catch (_: Exception) {
                Result.NetworkError
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(result) }
        }.start()
    }

    private fun httpGet(url: String): String? {
        val conn = java.net.URL(url).openConnection() as HttpsURLConnection
        return try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200)
                conn.inputStream.bufferedReader().readText().trim()
            else null
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPut(url: String, body: String) {
        val conn = java.net.URL(url).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode // 전송 확정
        } finally {
            conn.disconnect()
        }
    }
}
