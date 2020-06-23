package com.example.android.dreamteam.ui.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.FriendsListFragmentStatus
import com.example.android.dreamteam.utils.UserApiStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FriendsListViewModel(private val which: Int): ViewModel() {

    val _friends = MutableLiveData<List<User>>()
    val friends: LiveData<List<User>>
        get() = _friends

    val _navigateToUserFragment = MutableLiveData<String>()
    val navigateToUserFragment: LiveData<String>
        get() = _navigateToUserFragment

    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    val _fragmentStatus = MutableLiveData<FriendsListFragmentStatus>()
    val fragmentStatus: LiveData<FriendsListFragmentStatus>
        get() = _fragmentStatus

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference




    // load current user's friends list

    fun getUserFriends(uid: String) {
        var userReference = databaseRef.child("users").child(uid)
        var result: MutableList<User> = mutableListOf<User>()
        val userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                var myFriendsList = user.friendsList
                if(myFriendsList == null || myFriendsList.isEmpty()){
                    _fragmentStatus.value = FriendsListFragmentStatus.NO_FRIEND
                    _status.value = UserApiStatus.DONE
                }
                else{
                    for(entry in myFriendsList){
                        var friend = User().fromMap(entry.value as Map<String, Object>)
                        result.add(friend)
                    }
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = FriendsListFragmentStatus.DEFAULT
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
            }

        }

        userReference.addListenerForSingleValueEvent(userListener)
        _friends.value = result
        checkConnection()
    }

    // load current user's friend requests

    fun getUserFriendRequests(uid: String) {
        var userReference = databaseRef.child("users").child(uid)
        var result: MutableList<User> = mutableListOf<User>()
        val userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                var myRequestList = user.pendingRequests
                if(myRequestList.isNullOrEmpty()){
                    _fragmentStatus.value = FriendsListFragmentStatus.NO_REQUEST
                    _status.value = UserApiStatus.DONE
                }
                else{
                    for(entry in myRequestList){
                        var friend = User().fromMap(entry.value as Map<String, Object>)
                        result.add(friend)
                    }
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = FriendsListFragmentStatus.DEFAULT
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
            }

        }

        userReference.addListenerForSingleValueEvent(userListener)
        _friends.value = result
        checkConnection()
    }

    // check connection to Firebase

    fun checkConnection(){
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) _status.value = UserApiStatus.DONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
        _status.value = UserApiStatus.LOADING
        _fragmentStatus.value = FriendsListFragmentStatus.ERROR
    }

    fun onUserClicked(uid: String){
        _navigateToUserFragment.value = uid
    }

    fun onUserNavigated(){
        _navigateToUserFragment.value = null
    }

}