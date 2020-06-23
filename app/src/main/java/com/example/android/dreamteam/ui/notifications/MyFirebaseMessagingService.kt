package com.example.android.dreamteam.ui.notifications

import android.app.NotificationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.android.dreamteam.database.NotificationDatabase
import com.example.android.dreamteam.database.NotificationDatabaseDao
import com.example.android.dreamteam.model.Notification
import com.example.android.dreamteam.utils.sendNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

private val viewModelJob = Job()
private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
private lateinit var database: NotificationDatabaseDao

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage?.from}")


        // Check if message contains a data payload.
        remoteMessage?.data?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // create and put notification into local database
            var notificationId = Random().nextInt()
            var myself = FirebaseAuth.getInstance().currentUser!!.uid
            AndroidThreeTen.init(applicationContext)
            var notificationToDatabase = Notification()
            notificationToDatabase.id = notificationId
            notificationToDatabase.message = it["body"]
            notificationToDatabase.path = it["path"]
            notificationToDatabase.pictureUrl = it["icon"]
            notificationToDatabase.target = it["uid"]
            notificationToDatabase.uid = myself
            database = NotificationDatabase.getInstance(applicationContext).notificationDatabaseDao
            coroutineScope.launch{
                database.insert(notificationToDatabase)
                if(database.countNotifications() > 100){
                    database.clearNotifications(myself)
                }
            }

            sendNotification(remoteMessage.data)
        }
        // [END receive_message]
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     */
    private fun sendNotification(data: Map<String, String>) {
        val notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
        notificationManager.sendNotification(data, applicationContext)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}

