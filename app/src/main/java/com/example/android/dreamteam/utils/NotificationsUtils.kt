package com.example.android.dreamteam.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.android.dreamteam.HomeActivity
import com.example.android.dreamteam.R
import java.util.*

//extend sendNotification function to send notifications

fun NotificationManager.sendNotification(data: Map<String, String>, applicationContext: Context){

    var notificationId = Random().nextInt()

    var target: String? = null

    //process data to build the intent
    target = data["uid"]

    val contentIntent = Intent(applicationContext, HomeActivity::class.java)
    contentIntent.putExtra("uid", target)

    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        notificationId,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(applicationContext,
        applicationContext.getString(R.string.channel_ID))
        // icon, title, text, intent of the notification
        .setSmallIcon(R.drawable.match_marker)
        .setContentTitle(data["title"])
        .setContentText(data["body"])
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)

    //now send notification
    notify(notificationId, builder.build())
}

fun NotificationManager.cancelNotifications(){
    cancelAll()
}