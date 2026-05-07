package com.callbacksms.app.service

import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

object SmsManagerCompat {
    @Suppress("DEPRECATION")
    fun get(): SmsManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
            if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        }

        return SmsManager.getDefault()
    }
}
