package com.example.android.dreamteam.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FriendsListViewModelFactory(
    private val which: Int): ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(FriendsListViewModel::class.java)) {
                return FriendsListViewModel(which) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
}