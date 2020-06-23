package com.example.android.dreamteam.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.utils.Converters

@Database(entities = [Match::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MatchDatabase: RoomDatabase() {

    abstract val matchDatabaseDao: MatchDatabaseDao

    companion object{

        @Volatile
        private var INSTANCE: MatchDatabase? = null

        fun getInstance(context: Context) : MatchDatabase{
            synchronized(this){
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            MatchDatabase::class.java,
                            "match_database"
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