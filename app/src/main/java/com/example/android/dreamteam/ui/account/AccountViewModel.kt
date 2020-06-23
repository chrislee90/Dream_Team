package com.example.android.dreamteam.ui.account

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.database.UserDatabaseDao
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.UserApiStatus
import com.example.android.dreamteam.utils.UserFriendshipStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountViewModel(private val uid: String,
                       val database: UserDatabaseDao,
                       application: Application) : AndroidViewModel(application) {

    // user displayable values

    private val _picture = MutableLiveData<String>()
    val picture: LiveData<String>
        get() = _picture

    private val _name = MutableLiveData<String>()
    val name: LiveData<String>
        get() = _name

    private val _email = MutableLiveData<String>()
    val email: LiveData<String>
        get() = _email

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String>
        get() = _phoneNumber

    private val _rating = MutableLiveData<Double>()
    val rating: LiveData<Double>
        get() = _rating

    private val _role = MutableLiveData<String>()
    val role: LiveData<String>
        get() = _role

    private val _info = MutableLiveData<String>()
    val info: LiveData<String>
        get() = _info

    // navigation
    private val _navigateToEditAccount = MutableLiveData<String>()
    val navigateToEditAccount: LiveData<String>
        get() = _navigateToEditAccount

    // friendhsip status and network status

    val _friendshipStatus = MutableLiveData<UserFriendshipStatus>()
    val friendshipStatus: LiveData<UserFriendshipStatus>
        get() = _friendshipStatus

    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    // privacy
    private val _phonePublic = MutableLiveData<Boolean>()
    val phonePublic: LiveData<Boolean>
        get() = _phonePublic

    private val _emailPublic = MutableLiveData<Boolean>()
    val emailPublic: LiveData<Boolean>
        get() = _emailPublic

    // handle database
    private val myself = FirebaseAuth.getInstance().currentUser!!
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference

    // local user
    private var localUser: User? = null


    init {
        if (myself.uid != uid) {
            getUserByUid(uid)
            friendshipStatus()
        } else {
            _friendshipStatus.value =
                UserFriendshipStatus.MYSELF
            coroutineScope.launch{
                val isUserIn = database.isUserIn()
                if(isUserIn>0){
                    localUser = database.getByUid(uid)
                    displayUserOffline(localUser!!)
                } else {
                    getUserByUid(uid)
                }
            }
        }
    }

    fun displayUserOffline(user: User){
        _name.value = user.name
        _email.value = user.email
        _phoneNumber.value = user.phoneNumber
        _picture.value = user.picture
        _rating.value = user.rating
        _role.value = user.role
        _info.value = user.info
        _phonePublic.value = user.phonePublic
        _emailPublic.value = user.emailPublic
        _status.value = UserApiStatus.DONE
    }

    fun getUserByUid(uid: String) {
        var userReference = databaseRef.child("users").child(uid)
        val userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                _status.value = UserApiStatus.DONE
                val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                user?.let {
                    _name.value = user.name
                    _email.value = user.email
                    _phoneNumber.value = user.phoneNumber
                    _picture.value = user.picture
                    _rating.value = user.rating
                    _role.value = user.role
                    _info.value = user.info
                    _phonePublic.value = user.phonePublic
                    _emailPublic.value = user.emailPublic
                }
                if (uid == myself.uid) {
                    coroutineScope.launch {
                        database.insert(user)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
            }

        }

        userReference.addListenerForSingleValueEvent(userListener)
        checkConnection()

    }

    // check connection to Firebase

    fun checkConnection(){
        _status.value = UserApiStatus.LOADING
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    _status.value = UserApiStatus.DONE
                } else {
                    _status.value = UserApiStatus.ERROR
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
    }

    // check friendship status

    fun friendshipStatus() {
        var userFriendsListRef = databaseRef.child("users").child(myself.uid)
            .child("friendsList").orderByChild("uid").equalTo(uid)
        var userPendingListRef = databaseRef.child("users").child(myself.uid)
            .child("pendingList").orderByChild("uid").equalTo(uid)
        var userRequestingListRef = databaseRef.child("users").child(myself.uid)
            .child("pendingRequests").orderByChild("uid").equalTo(uid)

        val requestListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) _friendshipStatus.value = UserFriendshipStatus.REQUESTING
                else _friendshipStatus.value = UserFriendshipStatus.NONE

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("CANCELLED", databaseError.message)
            }
        }

        val pendingListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) _friendshipStatus.value = UserFriendshipStatus.PENDING
                else userRequestingListRef.addValueEventListener(requestListener)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("CANCELLED", databaseError.message)
            }
        }


        val friendListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) _friendshipStatus.value = UserFriendshipStatus.FRIEND
                else userPendingListRef.addValueEventListener(pendingListener)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("CANCELLED", databaseError.message)
            }
        }

        userFriendsListRef.addValueEventListener(friendListener)

    }


    // onClick Button: 'richiedi amicizia'

    fun friendRequest() {
        var myselfPendingList = databaseRef.child("users").child(myself.uid)
            .child("pendingList")
        var friendPendingRequests = databaseRef.child("users").child(uid)
            .child("pendingRequests")

        myselfPendingList.child(uid).child("uid").setValue(uid)
        friendPendingRequests.child(myself.uid).child("uid").setValue(myself.uid)
        friendPendingRequests.child(myself.uid).child("picture").setValue(myself.photoUrl.toString())
        friendPendingRequests.child(myself.uid).child("name").setValue(myself.displayName)

        friendshipStatus()
    }

    //onClick Button: 'rifiuta amicizia'

    fun denyFriendship() {
        var userPendingRequests = databaseRef.child("users").child(myself.uid).
            child("pendingRequests").child(uid)
        var friendPendingList = databaseRef.child("users").child(uid).
            child("pendingList").child(myself.uid)
        userPendingRequests.removeValue()
        friendPendingList.removeValue()

        friendshipStatus()
    }

    // onClick button: 'accetta richiesta'

    fun addFriend(){
        var userPendingRequests = databaseRef.child("users").child(myself.uid).
            child("pendingRequests").child(uid)
        var friendPendingList = databaseRef.child("users").child(uid).
            child("pendingList").child(myself.uid)
        userPendingRequests.removeValue()
        friendPendingList.removeValue()

        var userFriendsList = databaseRef.child("users").child(myself.uid).
            child("friendsList")
        var friendFriendsList = databaseRef.child("users").child(uid).
            child("friendsList")

        userFriendsList.child(uid).child("uid").setValue(uid)
        userFriendsList.child(uid).child("name").setValue(_name.value)
        userFriendsList.child(uid).child("picture").setValue(_picture.value)
        friendFriendsList.child(myself.uid).child("uid").setValue(myself.uid)
        friendFriendsList.child(myself.uid).child("name").setValue(myself.displayName)
        friendFriendsList.child(myself.uid).child("picture").setValue(myself.photoUrl.toString())


        friendshipStatus()
    }

    // onClick button: 'rimuovi amico'

    fun deleteFriend() {

        var userFriendsList = databaseRef.child("users").child(myself.uid).
            child("friendsList").child(uid)
        var friendFriendsList = databaseRef.child("users").child(uid).
            child("friendsList").child(myself.uid)

        userFriendsList.removeValue()
        friendFriendsList.removeValue()

        friendshipStatus()
    }

    // onClick button: 'modifica profilo'
    fun editProfile() {
        _navigateToEditAccount.value = uid
    }


    // on Navigated to Edit
    fun onUserNavigated(){
        _navigateToEditAccount.value = null
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}


