package com.callbacksms.app.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection

object DeviceAuth {

    const val FIREBASE_DB_URL = "https://callback-fc822-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val ADMIN_CODE = "482655"

    private const val PREF_NAME = "device_auth"
    private const val KEY_LICENSE = "license_key"

    fun isConfigured() = FIREBASE_DB_URL.isNotBlank()
    fun isAdminCode(key: String) = key.uppercase().trim() == ADMIN_CODE

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

    // ─── 라이선스 검증 ───────────────────────────────────────────────

    sealed class Result {
        object Allowed : Result()
        data class Denied(val reason: String) : Result()
        object NetworkError : Result()
    }

    fun validate(context: Context, licenseKey: String, onResult: (Result) -> Unit) {
        if (isAdminCode(licenseKey)) { onResult(Result.Allowed); return }
        if (!isConfigured()) { onResult(Result.Allowed); return }

        val key = licenseKey.uppercase().trim()
        val deviceId = getDeviceId(context)

        Thread {
            val result = try {
                val licenseJson = httpGet("$FIREBASE_DB_URL/licenses/$key.json")
                    ?: return@Thread post { onResult(Result.Denied("유효하지 않은 코드입니다")) }

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
                            if (!alreadyRegistered) {
                                // 기기 정보 포함해서 등록
                                val deviceInfo = JSONObject().apply {
                                    put("model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                                    put("registeredAt", System.currentTimeMillis())
                                }.toString()
                                httpPut("$FIREBASE_DB_URL/licenses/$key/devices/$deviceId.json", deviceInfo)
                            }
                            Result.Allowed
                        }
                    }
                }
            } catch (_: Exception) {
                Result.NetworkError
            }
            post { onResult(result) }
        }.start()
    }

    // ─── 관리자용 CRUD ───────────────────────────────────────────────

    data class DeviceEntry(
        val id: String,
        val model: String,
        val registeredAt: Long
    )

    data class LicenseInfo(
        val code: String,
        val active: Boolean,
        val maxDevices: Int,
        val note: String,
        val deviceCount: Int,
        val devices: List<DeviceEntry>
    )

    fun getAllLicenses(onResult: (List<LicenseInfo>?) -> Unit) {
        Thread {
            val result = try {
                val json = httpGet("$FIREBASE_DB_URL/licenses.json")
                when {
                    json == null -> null
                    json == "null" -> emptyList()
                    else -> {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().map { key ->
                            val item = obj.getJSONObject(key)
                            val devicesObj = item.optJSONObject("devices")
                            val deviceList = mutableListOf<DeviceEntry>()
                            devicesObj?.keys()?.forEach { deviceId ->
                                val v = devicesObj.opt(deviceId)
                                val (model, registeredAt) = if (v is JSONObject) {
                                    v.optString("model", "") to v.optLong("registeredAt", 0)
                                } else {
                                    "" to 0L
                                }
                                deviceList.add(DeviceEntry(deviceId, model, registeredAt))
                            }
                            LicenseInfo(
                                code = key,
                                active = item.optBoolean("active", false),
                                maxDevices = item.optInt("maxDevices", 1),
                                note = item.optString("note", ""),
                                deviceCount = deviceList.size,
                                devices = deviceList.sortedBy { it.registeredAt }
                            )
                        }.sortedBy { it.code }.toList()
                    }
                }
            } catch (_: Exception) { null }
            post { onResult(result) }
        }.start()
    }

    fun createLicense(code: String, maxDevices: Int, note: String, onResult: (Boolean) -> Unit) {
        Thread {
            val result = try {
                val body = JSONObject().apply {
                    put("active", true)
                    put("maxDevices", maxDevices)
                    if (note.isNotBlank()) put("note", note)
                }.toString()
                httpPut("$FIREBASE_DB_URL/licenses/${code.uppercase().trim()}.json", body)
                true
            } catch (_: Exception) { false }
            post { onResult(result) }
        }.start()
    }

    fun setLicenseActive(code: String, active: Boolean, onResult: (Boolean) -> Unit) {
        Thread {
            val result = try {
                httpPatch("$FIREBASE_DB_URL/licenses/$code.json", """{"active":$active}""")
                true
            } catch (_: Exception) { false }
            post { onResult(result) }
        }.start()
    }

    fun deleteLicense(code: String, onResult: (Boolean) -> Unit) {
        Thread {
            val result = try {
                httpDelete("$FIREBASE_DB_URL/licenses/$code.json")
                true
            } catch (_: Exception) { false }
            post { onResult(result) }
        }.start()
    }

    fun removeDevice(licenseCode: String, deviceId: String, onResult: (Boolean) -> Unit) {
        Thread {
            val result = try {
                httpDelete("$FIREBASE_DB_URL/licenses/$licenseCode/devices/$deviceId.json")
                true
            } catch (_: Exception) { false }
            post { onResult(result) }
        }.start()
    }

    // ─── HTTP 헬퍼 ───────────────────────────────────────────────────

    private fun post(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(block)
    }

    private fun httpGet(url: String): String? {
        val conn = java.net.URL(url).openConnection() as HttpsURLConnection
        return try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText().trim()
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
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPatch(url: String, body: String) {
        val conn = java.net.URL(url).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun httpDelete(url: String) {
        val conn = java.net.URL(url).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "DELETE"
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
