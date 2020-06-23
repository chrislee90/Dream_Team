package com.example.android.dreamteam.ui.account

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.dreamteam.database.UserDatabaseDao

class EditAccountViewModelFactory(
    private val uid: String,
    private val database: UserDatabaseDao,
    private val application: Application
): ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(EditAccountViewModel::class.java)) {
            return EditAccountViewModel(
                uid,
                database,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}