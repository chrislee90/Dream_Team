package com.example.android.dreamteam.ui.match

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.dreamteam.database.UserDatabaseDao
import com.google.android.gms.maps.SupportMapFragment


class MatchViewModelFactory(
    private val matchId: String,
    private val path: String,
    val database: UserDatabaseDao,
    val application: Application,
    private val mapFrag : SupportMapFragment
): ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MatchViewModel::class.java)) {
            return MatchViewModel(
                matchId,
                path,
                database,
                application,
                mapFrag
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}