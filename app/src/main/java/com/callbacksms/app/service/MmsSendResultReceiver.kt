package com.callbacksms.app.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class MmsSendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.getStringExtra(EXTRA_FILE_PATH)?.let { path ->
            runCatching { File(path).delete() }
        }

        if (resultCode == Activity.RESULT_OK) return

        val to = intent.getStringExtra(EXTRA_TO) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        runCatching {
            val sms = SmsManagerCompat.get()
            val parts = sms.divideMessage(text)
            if (parts.size == 1) {
                sms.sendTextMessage(to, null, text, null, null)
            } else {
                sms.sendMultipartTextMessage(to, null, parts, null, null)
            }
        }
    }

    companion object {
        const val ACTION_MMS_SENT = "com.callbacksms.app.action.MMS_SENT"
        const val EXTRA_TO = "extra_to"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
}
