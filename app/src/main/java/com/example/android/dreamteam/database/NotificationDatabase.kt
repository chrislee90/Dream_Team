package com.example.android.dreamteam.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.android.dreamteam.model.Notification
import com.example.android.dreamteam.utils.Converters

@Database(entities = [Notification::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NotificationDatabase: RoomDatabase() {

    abstract val notificationDatabaseDao: NotificationDatabaseDao

    companion object{

        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getInstance(context: Context) : NotificationDatabase{
            synchronized(this){
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        NotificationDatabase::class.java,
                        "notification_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }

    }
}
