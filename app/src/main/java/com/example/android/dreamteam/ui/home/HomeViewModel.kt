package com.example.android.dreamteam.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.database.MatchDatabaseDao
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.utils.FragmentStatus
import com.example.android.dreamteam.utils.UserApiStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeViewModel (
    val database: MatchDatabaseDao,
    application: Application): AndroidViewModel(application) {


    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    val _homeFragmentStatus = MutableLiveData<FragmentStatus>()
    val homeFragmentStatus: LiveData<FragmentStatus>
        get() = _homeFragmentStatus

    private val _navigateToMatchFragment = MutableLiveData<String>()
    val navigateToMatchFragment: LiveData<String>
            get() = _navigateToMatchFragment

    val _path = MutableLiveData<String>()
    val path: LiveData<String>
        get() = _path

    // handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference

    private val _search = MutableLiveData<MutableList<Match>>()
    val search: LiveData<MutableList<Match>>
        get() = _search

    init {
        _homeFragmentStatus.value = FragmentStatus.DEFAULT
        _status.value = UserApiStatus.DONE
    }


    fun getMatchesByName(query: String){
        _status.value = UserApiStatus.LOADING
        var result = mutableListOf<Match>()
        var matchesRef = databaseRef.child("publicMatches").orderByChild("name")
            .startAt(query)
            .endAt(query+"\uf8ff")
        val searchListener = object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.children.toList().isEmpty()) _homeFragmentStatus.value = FragmentStatus.NO_RESULT
                else {
                    _homeFragmentStatus.value = FragmentStatus.SEARCHING
                    for(snapshot in dataSnapshot.children){
                        var match = Match().fromMap(snapshot.value as Map<String, Object>)
                        result.add(match)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("CANCELLED", databaseError.message)
            }
        }

        result.clear()
        matchesRef.addListenerForSingleValueEvent(searchListener)
        _search.value = result
        checkConnection()
    }

    // check connection to Firebase

    fun checkConnection(){
        Thread.sleep(1500)
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    _status.value = UserApiStatus.DONE
                } else {
                    _status.value = UserApiStatus.ERROR
                    _homeFragmentStatus.value = FragmentStatus.ERROR
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
    }

    fun closeSearch(){
        _homeFragmentStatus.value = FragmentStatus.DEFAULT
    }

    fun onMatchClicked(matchId: String, isPublic: Boolean){
        if(isPublic) _path.value = "publicMatches"
        else _path.value = "privateMatches"
        _navigateToMatchFragment.value = matchId
    }

    fun onMatchNavigated(){
        _navigateToMatchFragment.value = null
        _path.value = null
    }

}