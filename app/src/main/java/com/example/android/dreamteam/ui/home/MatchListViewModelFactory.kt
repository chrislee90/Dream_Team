package com.example.android.dreamteam.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MatchListViewModelFactory(
    private val which: Int): ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MatchListViewModel::class.java)) {
            return MatchListViewModel(which) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}