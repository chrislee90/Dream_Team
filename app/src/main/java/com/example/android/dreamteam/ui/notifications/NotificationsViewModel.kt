package com.example.android.dreamteam.ui.notifications

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.database.NotificationDatabaseDao
import com.example.android.dreamteam.model.Notification
import com.example.android.dreamteam.utils.FragmentStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val viewModelJob = Job()
private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

private const val USER = "/users/"

class NotificationsViewModel(val database: NotificationDatabaseDao,
application: Application
): AndroidViewModel(application) {

    val _notificationsFragmentStatus = MutableLiveData<FragmentStatus>()
    val notificationsFragmentStatus: LiveData<FragmentStatus>
        get() = _notificationsFragmentStatus

    var myself = FirebaseAuth.getInstance().currentUser!!.uid

    var notifications = database.getAllNotifications(myself)

    val _navigateToUser = MutableLiveData<String>()
    val navigateToUser: LiveData<String>
        get() = _navigateToUser

    val _navigateToMatch = MutableLiveData<String>()
    val navigateToMatch: LiveData<String>
        get() = _navigateToMatch

    var pathToMatch: String? = null


    init{
        _notificationsFragmentStatus.value = FragmentStatus.DEFAULT
        observeState()
    }

    fun updateNotifications(){
        notifications = database.getAllNotifications(myself)
        observeState()
    }

    fun observeState(){
        coroutineScope.launch {
            val notificationsNumber = database.countNotifications()
            if(notificationsNumber<1) _notificationsFragmentStatus.value = FragmentStatus.NO_RESULT
            else _notificationsFragmentStatus.value = FragmentStatus.DEFAULT
        }
    }

    fun onNotificationClicked(target: String, id: Int, path: String){
        if(path == USER) {
            _navigateToUser.value = target
        }
        else {
            pathToMatch = path
            _navigateToMatch.value = target
        }
        coroutineScope.launch{
            var notificationToUpdate = database.get(id)
            notificationToUpdate!!.checked = true
            database.update(notificationToUpdate!!)
        }
    }

    fun onNavigatedUser(){
        _navigateToUser.value = null
    }

    fun onNavigatedMatch(){
        _navigateToMatch.value = null
        pathToMatch = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}