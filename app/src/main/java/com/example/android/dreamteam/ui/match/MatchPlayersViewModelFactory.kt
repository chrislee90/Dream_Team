package com.example.android.dreamteam.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MatchPlayersViewModelFactory(
    private val matchId: String,
    private val path: String,
    private val which: String
): ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MatchPlayersViewModel::class.java)) {
            return MatchPlayersViewModel(
                matchId,
                path,
                which
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}