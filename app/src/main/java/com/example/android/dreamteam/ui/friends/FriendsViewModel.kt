package com.example.android.dreamteam.ui.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.FragmentStatus
import com.example.android.dreamteam.utils.UserApiStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.Thread.sleep

class FriendsViewModel : ViewModel() {

    val _search = MutableLiveData<MutableList<User>>()
    val search: LiveData<MutableList<User>>
        get() = _search

    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    val _fragmentStatus = MutableLiveData<FragmentStatus>()
    val fragmentStatus: LiveData<FragmentStatus>
        get() = _fragmentStatus

    val _navigateToUserFragment = MutableLiveData<String>()
    val navigateToUserFragment: LiveData<String>
        get() = _navigateToUserFragment


    // uid of current user
    private val myself = FirebaseAuth.getInstance().currentUser!!.uid

    // handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference

    init {
        _fragmentStatus.value = FragmentStatus.DEFAULT
        _status.value = UserApiStatus.DONE
    }


    // search friends function

    fun getUsersByName(name:String) {
        _status.value = UserApiStatus.LOADING
        var result = mutableListOf<User>()
        var usersRef = databaseRef.child("users").orderByChild("name")
            .startAt(name)
            .endAt(name+"\uf8ff")
        val searchListener = object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.children.toList().isEmpty()) _fragmentStatus.value = FragmentStatus.NO_RESULT
                else {
                    _fragmentStatus.value = FragmentStatus.SEARCHING
                    for(snapshot in dataSnapshot.children){
                        var user = User().fromMap(snapshot.value as Map<String, Object>)
                        if(user.friendsList != null && user.friendsList!!.isNotEmpty()){
                            if(user.friendsList!!.contains(myself)) user.friend = true
                        }
                        result.add(user)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("CANCELLED", databaseError.message)
            }
        }
        result.clear()
        usersRef.addListenerForSingleValueEvent(searchListener)
        _search.value = result
        checkConnection()
    }


    // check connection to Firebase

    fun checkConnection(){
        sleep(1500)
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    _status.value = UserApiStatus.DONE
                } else {
                    _status.value = UserApiStatus.ERROR
                    _fragmentStatus.value = FragmentStatus.ERROR
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
    }


    // when you are done searching friends, go back to visualize friends fragment
    fun closeSearch(){
        _fragmentStatus.value = FragmentStatus.DEFAULT
    }

    // what to do when an user is clicked
    fun onUserClicked(uid: String){
        _navigateToUserFragment.value = uid
    }

    //when user has navigated, stop
    fun onUserNavigated(){
        _navigateToUserFragment.value = null
    }
}
