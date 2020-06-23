package com.example.android.dreamteam.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.android.dreamteam.model.Notification

@Dao
interface NotificationDatabaseDao {

    @Insert
    suspend fun insert(notification: Notification)

    @Update
    suspend fun update(notification: Notification)

    @Delete
    suspend fun delete(notification: Notification)

    @Query("SELECT * from notification_table WHERE id = :key")
    suspend fun get(key: Int): Notification?

    @Query("DELETE FROM notification_table")
    suspend fun clear()

    @Query("SELECT * FROM notification_table WHERE uid = :uid ORDER BY date DESC")
    fun getAllNotifications(uid: String): LiveData<List<Notification>>

    @Query("SELECT COUNT(*) from notification_table ")
    suspend fun countNotifications(): Int

    @Query("DELETE FROM notification_table WHERE uid = :uid AND checked")
    suspend fun clearNotifications(uid: String)
}