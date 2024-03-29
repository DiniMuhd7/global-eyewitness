package com.dinisoft.eyewitness.setting
/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

import android.app.Application
import android.content.Context
import android.util.Log
import com.onesignal.OSNotification
import com.onesignal.OneSignal

class OnesignalMessagingService: Application() {
    val TAG = "---Onesignal"
    lateinit var mContext: Context

    override fun onCreate() {
        super.onCreate()
        mContext = this
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(OneSignalNotificationOpenedHandler(mContext))
                .autoPromptLocation(true)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .init()

        OneSignal.idsAvailable { userId, registrationId -> Log.d(TAG, ""+userId) }
    }

    private inner class NotificationReceivedHandler : OneSignal.NotificationReceivedHandler {
        override fun notificationReceived(notification: OSNotification) {
            // receive a notification
        }
    }
}