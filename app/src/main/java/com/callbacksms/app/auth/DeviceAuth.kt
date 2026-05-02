package com.callbacksms.app.auth

import android.content.Context
import android.provider.Settings
import javax.net.ssl.HttpsURLConnection

object DeviceAuth {

    // ★ Firebase 콘솔에서 Realtime Database URL 복사 후 여기 붙여넣기
    //   예: "https://my-project-default-rtdb.firebaseio.com"
    //   빈 문자열이면 인증 비활성화 (개발용)
    private const val FIREBASE_DB_URL = ""

    private const val PREF_NAME = "device_auth"
    private const val KEY_ALLOWED = "allowed"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val OFFLINE_GRACE_MS = 7 * 24 * 60 * 60 * 1000L // 오프라인 허용 7일

    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    // Firebase URL 미설정이면 인증 없이 통과 (개발·디버그용)
    fun isConfigured() = FIREBASE_DB_URL.isNotBlank()

    fun check(context: Context, onResult: (allowed: Boolean) -> Unit) {
        if (!isConfigured()) { onResult(true); return }

        val deviceId = getDeviceId(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        Thread {
            val allowed = try {
                val url = "$FIREBASE_DB_URL/allowed/$deviceId.json"
                val conn = java.net.URL(url).openConnection() as HttpsURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                val body = if (code == 200)
                    conn.inputStream.bufferedReader().readText().trim()
                else "null"
                conn.disconnect()

                val result = body == "true"
                prefs.edit()
                    .putBoolean(KEY_ALLOWED, result)
                    .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                    .apply()
                result
            } catch (_: Exception) {
                // 네트워크 오류 → 마지막 허가 결과로 폴백 (7일 이내)
                val lastAllowed = prefs.getBoolean(KEY_ALLOWED, false)
                val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
                lastAllowed && (System.currentTimeMillis() - lastCheck < OFFLINE_GRACE_MS)
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(allowed) }
        }.start()
    }
}
