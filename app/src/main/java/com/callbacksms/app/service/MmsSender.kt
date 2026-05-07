package com.callbacksms.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.android.mms.MMSPart
import com.klinker.android.send_message.Transaction
import java.io.ByteArrayOutputStream
import java.io.File

object MmsSender {
    private const val IMAGE_NAME = "image_0.jpg"
    private const val TEXT_NAME = "text_0.txt"

    fun send(context: Context, to: String, text: String, imagePath: String): Pair<Boolean, String?> {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return Pair(false, "이미지 파일 없음: $imagePath")

            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return Pair(false, "이미지 디코딩 실패 (포맷 불지원)")
            val imageBytes = compressToJpeg(bitmap)
            bitmap.recycle()

            val pdu = buildPdu(context, to, text, imageBytes)
            val tmpFile = File(context.cacheDir, "mms_${System.currentTimeMillis()}.mms")
            tmpFile.writeBytes(pdu)

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", tmpFile
            )
            grantUriToTelephony(context, uri)
            SmsManagerCompat.get().sendMultimediaMessage(
                context,
                uri,
                null,
                null,
                buildSentIntent(context, to, text, tmpFile)
            )

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun buildPdu(context: Context, to: String, text: String, imageBytes: ByteArray): ByteArray {
        val imagePart = MMSPart().apply {
            Name = IMAGE_NAME
            MimeType = "image/jpeg"
            Data = imageBytes
        }
        val textPart = MMSPart().apply {
            Name = TEXT_NAME
            MimeType = "text/plain"
            Data = text.toByteArray(Charsets.UTF_8)
        }

        return Transaction.getBytes(
            context,
            false,
            to,
            arrayOf(to),
            arrayOf(imagePart, textPart),
            null
        ).bytes
    }

    private fun buildSentIntent(
        context: Context,
        to: String,
        text: String,
        pduFile: File
    ): PendingIntent {
        val intent = Intent(context, MmsSendResultReceiver::class.java).apply {
            action = MmsSendResultReceiver.ACTION_MMS_SENT
            putExtra(MmsSendResultReceiver.EXTRA_TO, to)
            putExtra(MmsSendResultReceiver.EXTRA_TEXT, text)
            putExtra(MmsSendResultReceiver.EXTRA_FILE_PATH, pduFile.absolutePath)
        }
        return PendingIntent.getBroadcast(
            context,
            pduFile.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val maxPx = 1024
        val scale = minOf(maxPx.toFloat() / bitmap.width, maxPx.toFloat() / bitmap.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val out = ByteArrayOutputStream()
        var quality = 85
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 280 * 1024 && quality > 30) {
            out.reset()
            quality -= 10
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        if (scaled != bitmap) scaled.recycle()
        return out.toByteArray()
    }

    private fun grantUriToTelephony(context: Context, uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        listOf(
            "android",
            "com.android.phone",
            "com.android.providers.telephony",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        ).forEach { pkg ->
            try {
                context.grantUriPermission(pkg, uri, flag)
            } catch (_: Exception) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            android.provider.Telephony.Sms.getDefaultSmsPackage(context)?.let { pkg ->
                try {
                    context.grantUriPermission(pkg, uri, flag)
                } catch (_: Exception) {
                }
            }
        }
    }
}
