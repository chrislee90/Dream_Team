package com.example.android.dreamteam.ui.newMatch

import android.app.Application
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.MatchDatabaseDao
import com.example.android.dreamteam.database.UserDatabaseDao
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter


class NewMatchViewModel(
    val database: MatchDatabaseDao,
    val dataUser: UserDatabaseDao,
    application: Application) : AndroidViewModel(application) {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // user uid and database reference
    private var myself = FirebaseAuth.getInstance().currentUser!!
    private var uid = FirebaseAuth.getInstance().currentUser!!.uid
    private var databaseRef: DatabaseReference

    private val _match = MutableLiveData<Match>(Match())

    private var dateHolder: LocalDate = LocalDate.now()
    private var timeHolder: LocalTime = LocalTime.NOON

    //typeButton
    val matchTypeButton = MutableLiveData<Int>()

    private val _typeMatch = MutableLiveData<String>()
    val typeMatch: LiveData<String>
        get() = _typeMatch

    private val _maxNumber = MutableLiveData<Long>()
    val maxNumber: LiveData<Long>
        get() = _maxNumber

    //public or private switch
    val switchButton = MutableLiveData<Boolean>()

    private val _isPublic = MutableLiveData<Boolean>()
    val isPublic: LiveData<Boolean>
        get() = _isPublic

    val matchTitle = MutableLiveData<String>()
    val matchPlace = MutableLiveData<String>()
    val matchPrice = MutableLiveData<String>()
    val matchPlaceLatLng = MutableLiveData<String>()

    private val _dateString = MutableLiveData<String>()
    val dateString : LiveData<String>
        get() = _dateString

    private val _timeString = MutableLiveData<String>()
    val timeString: LiveData<String>
        get() = _timeString


    init{
        updateDateString()
        databaseRef = Firebase.database.reference
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun onSaveMatch(){
        uiScope.launch {
            _match.value?.place = matchPlace.value ?: ""
            _match.value?.placeLatLng = matchPlaceLatLng.value ?: "-34.397,150.644"
            _match.value?.name = matchTitle.value ?: "Event"
            _match.value?.ownerId = uid
            _match.value?.price = matchPrice.value?.toDouble() ?: 5.50
            onClickType()
            _match.value!!.type = typeMatch.value
            _match.value!!.max_number = maxNumber.value
            onSwitchPublic()
            _match.value!!.isPublic = isPublic.value
            createMatch()
            Toast.makeText(
                this@NewMatchViewModel.getApplication(),
                "Match created successfully! Check 'My Matches' Tab!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun createMatch(){
        withContext(Dispatchers.IO){
            var match = _match.value!!
            pushToFirebase(match)
            database.insert(_match.value!!)
        }
    }

    suspend fun pushToFirebase(match: Match){
        withContext(Dispatchers.IO) {
            var user = dataUser.getByUid(myself.uid)
            if (match.isPublic!!) pushToPublic(match, user)
            else pushToPrivate(match, user)
        }
    }

    fun pushToPublic(match: Match, user: User){
        var id = databaseRef.child("publicMatches").push().key
        match.id = id!!
        val matchValues = match.toMap()
        val childUpdates = HashMap<String, Any>()

        //put into database
        childUpdates.put("/publicMatches/"+id, matchValues)
        databaseRef.updateChildren(childUpdates)
        //update participants with owner
        databaseRef.child("publicMatches").child(id).child("participants").child(uid).
            child("uid").setValue(uid)
        databaseRef.child("publicMatches").child(id).child("participants").child(uid).
            child("name").setValue(user.name)
        databaseRef.child("publicMatches").child(id).child("participants").child(uid).
            child("picture").setValue(myself.photoUrl.toString())
        databaseRef.child("publicMatches").child(id).child("participants").child(uid).
            child("role").setValue(user.role)

        //put match into owner list of matches
        val userUpdates = HashMap<String, Any>()
        userUpdates.put("/users/"+uid+"/matches/"+id, matchValues)
        databaseRef.updateChildren(userUpdates)
    }

    fun pushToPrivate(match: Match, user: User){
        var id = databaseRef.child("privateMatches").push().key
        match.id = id!!
        val matchValues = match.toMap()
        val childUpdates = HashMap<String, Any>()

        //put into database
        childUpdates.put("/privateMatches/"+id, matchValues)
        databaseRef.updateChildren(childUpdates)
        //update participants with owner
        databaseRef.child("privateMatches").child(id).child("participants").child(uid).
            child("uid").setValue(uid)
        databaseRef.child("privateMatches").child(id).child("participants").child(uid).
            child("name").setValue(myself.displayName)
        databaseRef.child("privateMatches").child(id).child("participants").child(uid).
            child("picture").setValue(myself.photoUrl.toString())
        databaseRef.child("privateMatches").child(id).child("participants").child(uid).
            child("role").setValue(user.role)

        //put match into owner list of matches
        val userUpdates = HashMap<String, Any>()
        userUpdates.put("/users/"+uid+"/matches/"+id, matchValues)
        databaseRef.updateChildren(userUpdates)
    }

    fun updateDate(newDate : LocalDate){
        dateHolder = newDate
        _match.value?.date = LocalDateTime.of(newDate, timeHolder).atZone(ZoneId.systemDefault())
            .toEpochSecond()
        updateDateString()
    }

    fun updateTime(newTime : LocalTime){
        timeHolder = newTime
        _match.value?.date = LocalDateTime.of(dateHolder, newTime).atZone(ZoneId.systemDefault())
            .toEpochSecond()
        updateDateString()
    }

    fun getMatchDate() : LocalDateTime{
        var instant = Instant.ofEpochSecond(_match.value?.date!!)
        var date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return date!!
    }

    fun updateDateString(){
        var instant = Instant.ofEpochSecond(_match.value?.date!!)
        var date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        _dateString.value = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _timeString.value = timeHolder.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    fun onClickType(){
        when(matchTypeButton.value){
            R.id.five_a_side -> {
                _typeMatch.value = "Five-a-side"
                _maxNumber.value = 10
            }
            R.id.eight_a_side -> {
                _typeMatch.value = "Eight-a-side"
                _maxNumber.value = 16
            }
            R.id.football -> {
                _typeMatch.value = "Football"
                _maxNumber.value = 22
            }
            else -> {
                _typeMatch.value = "Five-a-side"
                _maxNumber.value = 10
            }
        }
    }

    fun onSwitchPublic(){
        when(switchButton.value){
            true -> _isPublic.value = true
            else -> _isPublic.value = false
        }
    }

}
