package com.callbacksms.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

object MmsSender {

    // Boolean: 성공 여부, String?: 실패 시 에러 메시지
    fun send(context: Context, to: String, text: String, imagePath: String): Pair<Boolean, String?> {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return Pair(false, "이미지 파일 없음: $imagePath")
            val imageBytes = imageFile.readBytes()
            val mimeType = when (imageFile.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }

            val pdu = buildPdu(to, text, imageBytes, mimeType)
            val tmpFile = File(context.cacheDir, "mms_out_${System.currentTimeMillis()}.mms")
            tmpFile.writeBytes(pdu)

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", tmpFile
            )

            grantUriToTelephony(context, uri)

            getSmsManager(context).sendMultimediaMessage(context, uri, null, null, null)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun grantUriToTelephony(context: Context, uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        listOf(
            "android",
            "com.android.phone",
            "com.android.providers.telephony",
            "com.android.mms",
            "com.android.mms.service"
        ).forEach { pkg ->
            try { context.grantUriPermission(pkg, uri, flag) } catch (_: Exception) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            android.provider.Telephony.Sms.getDefaultSmsPackage(context)?.let { pkg ->
                try { context.grantUriPermission(pkg, uri, flag) } catch (_: Exception) {}
            }
        }
    }

    private fun buildPdu(to: String, text: String, imageBytes: ByteArray, mimeType: String): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(0x8C); out.write(0x80)

        val txId = System.currentTimeMillis().toString().toByteArray(Charsets.US_ASCII)
        out.write(0x98)
        writeUintVar(out, txId.size + 1)
        out.write(txId); out.write(0)

        out.write(0x8D); out.write(0x90)

        val secs = System.currentTimeMillis() / 1000
        out.write(0x85); out.write(4)
        out.write((secs shr 24).toInt() and 0xFF)
        out.write((secs shr 16).toInt() and 0xFF)
        out.write((secs shr 8).toInt() and 0xFF)
        out.write(secs.toInt() and 0xFF)

        out.write(0x89); out.write(1); out.write(0x81)

        val toBytes = "$to/TYPE=PLMN".toByteArray(Charsets.US_ASCII)
        out.write(0x97)
        writeUintVar(out, toBytes.size + 1)
        out.write(toBytes); out.write(0)

        out.write(0x84); out.write(0xA3.toByte().toInt())

        writeUintVar(out, 2)

        val textBytes = text.toByteArray(Charsets.UTF_8)
        val textHdr = byteArrayOf(0x03, 0x83.toByte(), 0x01, 0xEA.toByte())
        writeUintVar(out, textHdr.size)
        writeUintVar(out, textBytes.size)
        out.write(textHdr)
        out.write(textBytes)

        val imgHdr = ByteArrayOutputStream().apply {
            write(mimeType.toByteArray(Charsets.US_ASCII))
            write(0)
        }.toByteArray()
        writeUintVar(out, imgHdr.size)
        writeUintVar(out, imageBytes.size)
        out.write(imgHdr)
        out.write(imageBytes)

        return out.toByteArray()
    }

    private fun writeUintVar(out: ByteArrayOutputStream, value: Int) {
        if (value < 128) { out.write(value); return }
        val bytes = mutableListOf<Int>()
        var v = value
        while (v > 0) { bytes.add(0, v and 0x7F); v = v ushr 7 }
        for (i in 0 until bytes.size - 1) out.write(bytes[i] or 0x80)
        out.write(bytes.last())
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()
}
