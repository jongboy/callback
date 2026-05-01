package com.callbacksms.app.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

object MmsSender {

    fun send(context: Context, to: String, text: String, imagePath: String): Boolean {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return false
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
            getSmsManager(context).sendMultimediaMessage(context, uri, null, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildPdu(to: String, text: String, imageBytes: ByteArray, mimeType: String): ByteArray {
        val out = ByteArrayOutputStream()

        // X-Mms-Message-Type: m-send-req
        out.write(0x8C); out.write(0x80)

        // X-Mms-Transaction-ID
        val txId = System.currentTimeMillis().toString().toByteArray(Charsets.US_ASCII)
        out.write(0x98)
        writeUintVar(out, txId.size + 1)
        out.write(txId); out.write(0)

        // X-Mms-MMS-Version: 1.0
        out.write(0x8D); out.write(0x90)

        // Date (4-byte unix seconds)
        val secs = System.currentTimeMillis() / 1000
        out.write(0x85); out.write(4)
        out.write((secs shr 24).toInt() and 0xFF)
        out.write((secs shr 16).toInt() and 0xFF)
        out.write((secs shr 8).toInt() and 0xFF)
        out.write(secs.toInt() and 0xFF)

        // From: insert-address
        out.write(0x89); out.write(1); out.write(0x81)

        // To: phone/TYPE=PLMN
        val toBytes = "$to/TYPE=PLMN".toByteArray(Charsets.US_ASCII)
        out.write(0x97)
        writeUintVar(out, toBytes.size + 1)
        out.write(toBytes); out.write(0)

        // Content-Type: application/vnd.wap.multipart.mixed (0x23 | 0x80 = 0xA3)
        out.write(0x84); out.write(0xA3.toByte().toInt())

        // Body: 2 parts
        writeUintVar(out, 2)

        // Part 1: text/plain; charset=utf-8
        val textBytes = text.toByteArray(Charsets.UTF_8)
        // Header: value-length(3) + text/plain(0x83) + charset(0x01) + UTF-8(0xEA)
        val textHdr = byteArrayOf(0x03, 0x83.toByte(), 0x01, 0xEA.toByte())
        writeUintVar(out, textHdr.size)
        writeUintVar(out, textBytes.size)
        out.write(textHdr)
        out.write(textBytes)

        // Part 2: image
        val imgTypeCode = when (mimeType) {
            "image/png" -> 0x9F  // image/png WSP short integer
            else -> 0x9E         // image/jpeg WSP short integer
        }
        val imgHdr = byteArrayOf(imgTypeCode.toByte())
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
