package com.example.android.dreamteam.ui.newMatch

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.dreamteam.database.MatchDatabaseDao
import com.example.android.dreamteam.database.UserDatabaseDao

class NewMatchViewModelFactory (
    private val dataSource: MatchDatabaseDao,
    private val dataUser: UserDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewMatchViewModel::class.java)) {
            return NewMatchViewModel(dataSource, dataUser, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}