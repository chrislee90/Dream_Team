package com.example.android.dreamteam.ui.account

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.UserDatabaseDao
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.UserApiStatus
import com.example.android.dreamteam.utils.UserRoleStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val NO_ROLE = "None"
private const val KP_ROLE = "Keeper"
private const val DF_ROLE = "Defender"
private const val MD_ROLE = "Midfielder"
private const val AT_ROLE = "Striker"

class EditAccountViewModel (private val uid: String,
                            val database: UserDatabaseDao,
                            application: Application
) : AndroidViewModel(application) {

    // Account editable fields

    val name = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()
    val role = MutableLiveData<String>()
    val roleButton = MutableLiveData<Int>()
    val info = MutableLiveData<String>()

    // handle network status

    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    private var databaseRef: DatabaseReference = Firebase.database.reference


    // handle role chosen

    private val _roleStatus = MutableLiveData<UserRoleStatus>()
    val roleStatus: LiveData<UserRoleStatus>
        get() = _roleStatus

    // handle navigation after update completed
    private val _navigateBackToAccount = MutableLiveData<String>()
    val navigateBackToAccount: LiveData<String>
        get() = _navigateBackToAccount

    init {
        getUserByUid()
    }

    private fun getUserByUid() {
        _status.value =
            UserApiStatus.LOADING
        var userReference = databaseRef.child("users").child(uid)
        val userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                _status.value = UserApiStatus.LOADING
                val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                user?.let {
                    name.value = user.name
                    if(user.phoneNumber != null) {
                        phoneNumber.value = user.phoneNumber
                    } else {
                        phoneNumber.value = ""
                    }
                    role.value = user.role
                    when (role.value) {
                        NO_ROLE -> _roleStatus.value = UserRoleStatus.NONE
                        KP_ROLE -> _roleStatus.value = UserRoleStatus.KEEPER
                        DF_ROLE -> _roleStatus.value = UserRoleStatus.DEF
                        MD_ROLE -> _roleStatus.value = UserRoleStatus.MID
                        AT_ROLE -> _roleStatus.value = UserRoleStatus.ATT
                    }
                    info.value = user.info
                    _status.value = UserApiStatus.DONE
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
            }

        }
        userReference.addListenerForSingleValueEvent(userListener)
        checkConnection()

    }

    fun updateUser(){
        onClickRole()
        _status.value =
            UserApiStatus.LOADING

        var userReference = databaseRef.child("users").child(uid)
        userReference.child("name").setValue(name.value)
        userReference.child("phone_number").setValue(phoneNumber.value)
        userReference.child("role").setValue(role.value)
        userReference.child("info").setValue(info.value)
        coroutineScope.launch{
            var user = database.getByUid(uid)
            user.name = name.value
            user.phoneNumber = phoneNumber.value
            user.role = role.value
            user.info = info.value
            database.update(user)
            _navigateBackToAccount.value = uid
            Toast.makeText(
                this@EditAccountViewModel.getApplication(),
                "Updated successfully.",
                Toast.LENGTH_LONG
            ).show()
        }

        _status.value = UserApiStatus.DONE
    }


    // check connection to Firebase

    fun checkConnection(){
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

    fun onNavigated(){
        _navigateBackToAccount.value = null
    }

    fun onClickRole(){
        when(roleButton.value){
            R.id.radioNull -> role.value = NO_ROLE
            R.id.radioKeeper -> role.value = KP_ROLE
            R.id.radioDefender -> role.value = DF_ROLE
            R.id.radioMid -> role.value = MD_ROLE
            R.id.radioAtt -> role.value = AT_ROLE
        }
    }


}