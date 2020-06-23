package com.example.android.dreamteam.ui.match

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.FriendsListFragmentStatus
import com.example.android.dreamteam.utils.MatchListPlayersStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private const val PLAYERS = "PLAYERS"
private const val FRIENDS = "FRIENDS"
private const val REQUESTS = "REQUESTS"
private const val JOIN = "JOIN"
private const val DENY = "DENY"
private const val NAVIGATE = "NAVIGATE"

class MatchPlayersViewModel(private val matchId: String,
                            val path: String,
                            private val which: String) : ViewModel() {


    val _match = MutableLiveData<Match>()
    val match: LiveData<Match>
        get() = _match

    val _players = MutableLiveData<List<User>>()
    val players: LiveData<List<User>>
        get() = _players

    val _navigateToUserFragment = MutableLiveData<String>()
    val navigateToUserFragment: LiveData<String>
        get() = _navigateToUserFragment

    val _fragmentStatus = MutableLiveData<FriendsListFragmentStatus>()
    val fragmentStatus: LiveData<FriendsListFragmentStatus>
        get() = _fragmentStatus

    val _listStatus = MutableLiveData<MatchListPlayersStatus>()
    val listStatus: LiveData<MatchListPlayersStatus>
        get() = _listStatus

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference
    var uid = FirebaseAuth.getInstance().currentUser!!.uid

    init{
        getMatchById()
    }

    fun getMatchById() {
        var matchReference = databaseRef.child(path).child(matchId)
        val matchListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) {
                    val matchSnapshot = dataSnapshot.value as Map<String, Object>
                    val matchRetrieved = Match().fromMap(matchSnapshot)
                    matchRetrieved?.let {
                        _match.value = matchRetrieved
                        when (which) {
                            PLAYERS -> showPlayers()
                            FRIENDS -> showMyFriends()
                            REQUESTS -> showRequests()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }
        }
        matchReference.addValueEventListener(matchListener)
        checkConnection()
    }

    fun showPlayers(){
        var result = mutableListOf<User>()
        for(entry in _match.value!!.participants!!){
            val user = User().fromMap(entry.value as Map<String, Object>)
            result.add(user)
        }
        _fragmentStatus.value = FriendsListFragmentStatus.DEFAULT
        _listStatus.value = MatchListPlayersStatus.SHOWING
        _players.value = result
    }

    fun showRequests(){
        var result = mutableListOf<User>()
        if(_match.value!!.pendingRequests.isNullOrEmpty()) _fragmentStatus.value = FriendsListFragmentStatus.NO_REQUEST
        else {
            for (entry in _match.value!!.pendingRequests!!) {
                val user = User().fromMap(entry.value as Map<String, Object>)
                result.add(user)
            }
            _fragmentStatus.value = FriendsListFragmentStatus.DEFAULT
            _players.value = result
            if(_match.value!!.numberParticipants!! < _match.value!!.max_number!!)
                _listStatus.value = MatchListPlayersStatus.ACCEPTING
            else
                _listStatus.value = MatchListPlayersStatus.FULL_REQUEST
        }
    }

    fun showMyFriends(){
        var userReference = databaseRef.child("users").child(uid)
        val userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                var myFriendsList = user.friendsList
                var result: MutableList<User> = mutableListOf<User>()
                _players.value = result
                if(myFriendsList == null || myFriendsList.isEmpty()){
                    _fragmentStatus.value = FriendsListFragmentStatus.NO_FRIEND
                }
                else{
                    _fragmentStatus.value = FriendsListFragmentStatus.DEFAULT
                    for(entry in myFriendsList){
                        var friend = User().fromMap(entry.value as Map<String, Object>)
                        if(match.value!!.participants!!.containsKey(friend.uid)) //none
                        else{
                            if(!_match.value!!.pendingRequests.isNullOrEmpty() &&
                                _match.value!!.pendingRequests!!.contains(friend.uid)) //none
                            else {
                                if(!_match.value!!.pendingList.isNullOrEmpty() &&
                                    _match.value!!.pendingList!!.contains(friend.uid)) //none
                                else result.add(friend) //then you can add it
                            }
                        }
                    }
                    _players.value = result
                    if(players.value.isNullOrEmpty())
                        _fragmentStatus.value = FriendsListFragmentStatus.NO_FRIEND
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
            }

        }

        userReference.addValueEventListener(userListener)
        if(_match.value!!.numberParticipants!! < _match.value!!.max_number!!)
            _listStatus.value = MatchListPlayersStatus.INVITING
        else
            _listStatus.value = MatchListPlayersStatus.FULL_INVITING
        checkConnection()
    }



    fun onUserClicked(uid: String, action: String){
        when(action){
            NAVIGATE -> _navigateToUserFragment.value = uid
            JOIN -> {
                if(which == REQUESTS) acceptRequest(uid)
                else if(which == FRIENDS) inviteFriend(uid)
            }
            DENY -> {
                if(which == REQUESTS) denyRequest(uid)
                else if (which == PLAYERS) expelPlayer(uid)
            }
        }
    }

    fun onUserNavigated(){
        _navigateToUserFragment.value = null
    }

    fun acceptRequest(uid: String){
        if(_match.value!!.numberParticipants!! < _match.value!!.max_number!!) {
            var newValue = _match.value!!.numberParticipants?.plus(1)
            _match.value!!.numberParticipants = newValue
            //put into database match updated
            val matchValues = _match.value!!.toMap()
            val childUpdates = HashMap<String, Any>()
            childUpdates["/$path/$matchId"] = matchValues
            databaseRef.updateChildren(childUpdates)

            //now get user from pending Requests
            var userMap = _match.value!!.pendingRequests!![uid] as Map<String, Any>
            //add it to the participants
            val matchUpdates = HashMap<String, Any>()
            matchUpdates["/$path/$matchId/participants/$uid"] = userMap
            databaseRef.updateChildren(matchUpdates)
            //erase user from pending Requests
            databaseRef.child(path).child(matchId).child("pendingRequests").child(uid).removeValue()
            //erase user from owner's pending Requests
            databaseRef.child("users").child(_match.value!!.ownerId!!).child("matches").
                child(matchId).child("pendingRequests").child(uid).removeValue()

            //now update friend's matches so he can see match from MyMatches
            var matchMap = match.value!!.toMap()
            var friendUpdates = HashMap<String, Any>()
            friendUpdates["/users/$uid/matches/$matchId"] = matchMap
            databaseRef.updateChildren(friendUpdates)

            //now update all the participants
            for(entry in _match.value!!.participants!!){
                var p_uid = entry.key
                val participantsUpdates = HashMap<String, Any>()
                participantsUpdates["/users/$p_uid/matches/$matchId"] = matchValues
                databaseRef.updateChildren(participantsUpdates)
            }
            //now update all the invited ones, if any
            if(!match.value!!.pendingList.isNullOrEmpty()) {
                for (entry in _match.value!!.pendingList!!) {
                    var p_uid = entry.key
                    val invitedUpdates = HashMap<String, Any>()
                    invitedUpdates["/users/$p_uid/matches/$matchId"] = matchValues
                    databaseRef.updateChildren(invitedUpdates)
                }
            }
            if(match.value!!.numberParticipants!! == match.value!!.max_number!!){
                autoClean()
                _listStatus.value = MatchListPlayersStatus.FULL_REQUEST
            } else
                _listStatus.value = MatchListPlayersStatus.ACCEPTING
        }
    }

    fun denyRequest(uid: String){
        //remove from pending requests
        databaseRef.child(path).child(matchId).child("pendingRequests").child(uid).removeValue()
        //now update for owner
        databaseRef.child("users").child(match.value!!.ownerId!!).child("matches").child(matchId).
            child("pendingRequests").child(uid).removeValue()
        //update global reference to match with the rejected id
        databaseRef.child(path).child(matchId).child("rejected").child(uid).setValue(uid)
    }

    fun inviteFriend(uid: String){
        if (match.value!!.numberParticipants!! < _match.value!!.max_number!!) {
            //prepare add friend to the list of pending lists, looking for its id
            var iterator = _players.value!!.iterator()
            var invitedFriend: User? = null
            while (iterator.hasNext() && invitedFriend == null) {
                var user = iterator.next()
                if (user.uid == uid) {
                    invitedFriend = user
                }
            }
            var invitedFriendMap = invitedFriend!!.toMap()
            var matchUpdates = HashMap<String, Any>()
            matchUpdates["/$path/$matchId/pendingList/$uid"] = invitedFriendMap
            databaseRef.updateChildren(matchUpdates)

            //now update for owner
            var ownerUpdates = HashMap<String, Any>()
            ownerUpdates.put(
                "/users/" + match.value!!.ownerId!! + "/matches/" + matchId + "/pendingList/" + uid,
                invitedFriendMap
            )
            databaseRef.updateChildren(ownerUpdates)

            //now update friend's matches so he can see match from MyMatches
            var matchMap = match.value!!.toMap()
            var friendUpdates = HashMap<String, Any>()
            friendUpdates.put("/users/" + uid + "/matches/" + matchId, matchMap)
            databaseRef.updateChildren(friendUpdates)
        }
        if(match.value!!.numberParticipants!! == match.value!!.max_number!!){
            autoClean()
            _listStatus.value = MatchListPlayersStatus.FULL_INVITING
        }
    }

    fun expelPlayer(uid: String){
        if(uid!=match.value!!.ownerId) { //can't remove owner!!
            var newValue = _match.value!!.numberParticipants?.minus(1)
            _match.value!!.numberParticipants = newValue
            //put into database match updated
            val matchValues = _match.value!!.toMap()
            val childUpdates = HashMap<String, Any>()
            childUpdates.put("/"+path+"/"+matchId, matchValues)
            databaseRef.updateChildren(childUpdates)

            //remove from global participants
            databaseRef.child(path).child(matchId).child("participants").child(uid).
                removeValue()
            //remove for every participant
            for(entry in _match.value!!.participants!!){
                var p_uid = entry.key
                databaseRef.child("users").child(p_uid).child("matches").child(matchId).
                    child("participants").child(uid).removeValue()
                databaseRef.child("users").child(p_uid).child("matches").child(matchId).
                    child("numberParticipants").setValue(newValue)
            }
            //remove for every pendingList friend
            if(!match.value!!.pendingList.isNullOrEmpty()) {
                for (entry in _match.value!!.pendingList!!) {
                    var p_uid = entry.key
                    databaseRef.child("users").child(p_uid).child("matches").child(matchId).
                        child("participants")
                        .child(uid).removeValue()
                    databaseRef.child("users").child(p_uid).child("matches").child(matchId).
                        child("numberParticipants").setValue(newValue)
                }
            }
            //remove match from player's list of matches
            databaseRef.child("users").child(uid).child("matches").child(matchId).
                    removeValue()
            //tell everyone he's gone forever
            databaseRef.child(path).child(matchId).child("removed").child(uid).setValue(uid)
        }
    }

    fun autoClean(){
        //when max number of players has been reached after interaction, automatically
        //clean every request/invite for the match - a notification will be sent
        //clean pendingList
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            //this should be necessary to send single notification to everyone
            for (entry in _match.value!!.pendingList!!) {
                var i_uid = entry.key
                databaseRef.child(path).child(matchId).child("pendingList").child(i_uid).
                    removeValue()
                //and reference to the invited ones
                databaseRef.child("users").child(i_uid).child("matches").child(matchId).
                    removeValue()
            }
            databaseRef.child("users").child(match.value!!.ownerId!!).child("matches").
                child(matchId).child("pendingList").removeValue()
        }
        //clean requestsList
        if(!match.value!!.pendingRequests.isNullOrEmpty()) {
            //this should be necessary to send single notification to everyone
            for (entry in _match.value!!.pendingRequests!!) {
                var p_uid = entry.key
                databaseRef.child(path).child(matchId).child("pendingRequests").child(p_uid).
                    removeValue()
            }
            databaseRef.child("users").child(match.value!!.ownerId!!).child("matches").
                child(matchId).child("pendingRequests").removeValue()
        }
    }

    // check connection to Firebase

    fun checkConnection(){
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                //TODO
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
    }

}
