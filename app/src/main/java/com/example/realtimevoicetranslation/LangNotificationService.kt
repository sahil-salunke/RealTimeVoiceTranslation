package com.example.realtimevoicetranslation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast

class LangNotificationService : NotificationListenerService() {

//    sbn.packageName => com.android.providers.downloads
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if(sbn?.packageName == "com.android.providers.downloads"){
            Toast.makeText(this, "Language is being downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("AppNotifiedStatusRate", "Notification from ${sbn?.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if(sbn?.packageName == "com.android.providers.downloads"){
            Toast.makeText(this, "Language downloaded successfully", Toast.LENGTH_SHORT).show()
        }
        Log.d("AppNotifiedStatusRate", "Removed notification from ${sbn?.packageName}")
    }
}