package com.example.android.dreamteam.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.android.dreamteam.model.Match

@Dao
interface MatchDatabaseDao {

    @Insert
    fun insert(match: Match)

    @Update
    fun update(match: Match)

    @Delete
    fun delete(match: Match)

    @Query("SELECT * from match_table WHERE matchId = :key")
    fun get(key: Long): Match?

    @Query("DELETE FROM match_table")
    fun clear()

    @Query("SELECT * FROM match_table ORDER BY date ASC")
    fun getAllMatches(): LiveData<List<Match>>

    @Query("SELECT * from match_table WHERE matchId = :key")
    fun getMatchWithId(key: Long): LiveData<Match>
}