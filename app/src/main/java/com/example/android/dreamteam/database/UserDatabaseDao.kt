package com.example.android.dreamteam.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.android.dreamteam.model.User

@Dao
interface UserDatabaseDao {

    @Insert
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("DELETE FROM user_table")
    suspend fun clear()

    @Query("SELECT * from user_table WHERE uid = :key")
    suspend fun getByUid(key: String): User

    @Query("SELECT COUNT(*) from user_table ")
    suspend fun isUserIn(): Int

    @Query("SELECT * from user_table WHERE uid = :key")
    suspend fun verifyByUid(key: String): User?

}