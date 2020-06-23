package com.example.android.dreamteam.ui.match

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.dreamteam.database.UserDatabaseDao
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.MatchFragmentStatus
import com.example.android.dreamteam.utils.MatchUserStatus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
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
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.lang.Thread.sleep

class MatchViewModel(
    var matchId: String, var path: String,
    val database: UserDatabaseDao,
    application: Application,
    mapFrag: SupportMapFragment //added input field
): AndroidViewModel(application), OnMapReadyCallback {

    private val context: Context
    lateinit var map: GoogleMap

    private val _match = MutableLiveData<Match>()
    val match: LiveData<Match>
        get() = _match

    private val _ownerName = MutableLiveData<String>()
    val ownerName: LiveData<String>
        get() = _ownerName

    private val _matchUserStatus = MutableLiveData<MatchUserStatus>()
    val matchUserStatus: LiveData<MatchUserStatus>
        get() = _matchUserStatus

    private val _navigateBackToHome = MutableLiveData<Boolean>()
    val navigateBackToHome: LiveData<Boolean>
        get() = _navigateBackToHome

    private val _navigateToPlayers = MutableLiveData<String>()
    val navigateToPlayers: LiveData<String>
        get() = _navigateToPlayers

    private val _navigateToFriends = MutableLiveData<String>()
    val navigateToFriends: LiveData<String>
        get() = _navigateToFriends

    private val _navigateToRequests = MutableLiveData<String>()
    val navigateToRequests: LiveData<String>
        get() = _navigateToRequests


    private val _fragmentStatus = MutableLiveData<MatchFragmentStatus>()
    val fragmentStatus: LiveData<MatchFragmentStatus>
        get() = _fragmentStatus

    //is map ready to be shown?
    private val _isMapReady = MutableLiveData<Boolean>()
    val isMapReady: LiveData<Boolean>
        get() = _isMapReady

    //is match ready?
    private val _isMatchReady = MutableLiveData<Boolean?>()
    val isMatchReady: LiveData<Boolean?>
        get() = _isMatchReady

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference
    private var myself = FirebaseAuth.getInstance().currentUser!!
    var uid = FirebaseAuth.getInstance().currentUser!!.uid

    //handle database operations
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    //today's date
    val today = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toDouble()

    private val _navigateToMaps =
        SingleLiveEvent<Any>()
    val navigateToMaps : LiveData<Any>
        get() = _navigateToMaps

    private val _mapIntent = MutableLiveData<Intent>()
    val mapIntent : LiveData<Intent>
        get() = _mapIntent

    init{
        getMatchById()
        //required to start Gmaps
        this.context = application
        mapFrag.getMapAsync(this)
    }



    fun getMatchById() {
        var matchReference = databaseRef.child(path).child(matchId)
        val matchListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) {
                    val matchSnapshot = dataSnapshot.value as Map<String, Object>
                    val matchRetrieved = Match().fromMap(matchSnapshot)
                    matchRetrieved.let {
                        _match.value = matchRetrieved
                        _isMapReady.value = true
                    }
                    if(path == "privateMatches"){
                        if(!matchRetrieved.participants!!.contains(uid) &&
                            (matchRetrieved.pendingRequests.isNullOrEmpty() || !matchRetrieved.pendingRequests!!.contains(uid)) &&
                            (matchRetrieved.pendingList.isNullOrEmpty() || !matchRetrieved.pendingList!!.contains(uid))){
                            _fragmentStatus.value = MatchFragmentStatus.ERROR
                        } else _fragmentStatus.value = MatchFragmentStatus.DEFAULT
                    } else  _fragmentStatus.value = MatchFragmentStatus.DEFAULT
                    getOwnerNameByUid()
                    checkMatchUserStatus()
                } else {
                    _fragmentStatus.value = MatchFragmentStatus.ERROR
                }
                checkConnection()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }
        }

        _fragmentStatus.value = MatchFragmentStatus.LOADING
        matchReference.addValueEventListener(matchListener)
    }

    fun getOwnerNameByUid(){
        var userReference = databaseRef.child("users").child(match.value!!.ownerId!!)
        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userSnapshot = dataSnapshot.value as Map<String, Object>
                val userRetrieved = User().fromMap(userSnapshot)
                userRetrieved?.let{
                    _ownerName.value = userRetrieved.name
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL USER", "FAIL:" + databaseError.message)
            }
        }

        userReference.addValueEventListener(userListener)
    }

    fun checkMatchUserStatus() {
        if (match.value!!.ownerId == uid) _matchUserStatus.value = MatchUserStatus.OWNER
        else {
            if(match.value!!.participants!!.contains(uid))
                _matchUserStatus.value = MatchUserStatus.PARTICIPANT
            else{
                if(!_match.value!!.pendingRequests.isNullOrEmpty() &&
                        _match.value!!.pendingRequests!!.contains(uid))
                    _matchUserStatus.value = MatchUserStatus.REQUESTING
                else {
                    if(!_match.value!!.pendingList.isNullOrEmpty() &&
                        _match.value!!.pendingList!!.contains(uid))
                        _matchUserStatus.value = MatchUserStatus.INVITED
                    else _matchUserStatus.value = MatchUserStatus.NONE
                }
            }
        }
        _isMatchReady.value = true
    }

    //clickable only when today < match.date
    fun changeConfirmStatus(){
        match.value!!.isConfirmed = !match.value!!.isConfirmed!!
        //now update
        databaseRef.child(path).child(matchId).child("isConfirmed")
            .setValue(match.value!!.isConfirmed)
    }

    fun deleteMatch(){
        //delete every reference of the participants
        for(entry in _match.value!!.participants!!){
            var uid = entry.key
            databaseRef.child("users").child(uid).child("matches")
                .child(_match.value!!.id!!).removeValue()
        }
        //delete every reference of the invited ones
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            for (entry in match.value!!.pendingList!!) {
                var uid = entry.key
                databaseRef.child("users").child(uid).child("matches")
                    .child(_match.value!!.id!!).removeValue()
            }
        }
        //now delete the match
        databaseRef.child(path).child(matchId).removeValue()
        Toast.makeText(
            this@MatchViewModel.getApplication(),
            "Match deleted successfully!",
            Toast.LENGTH_LONG
        ).show()

        //now navigate back to Home
        _navigateBackToHome.value = true
    }

    fun changePublishStatus(){
        var oldStatus = _match.value!!.isPublic!!
        //first delete every reference of the participants, this step is
        //just like "deleteMatch"
        for(entry in _match.value!!.participants!!){
            var uid = entry.key
            databaseRef.child("users").child(uid).child("matches")
                .child(_match.value!!.id).removeValue()
        }
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            for (entry in match.value!!.pendingList!!) {
                var uid = entry.key
                databaseRef.child("users").child(uid).child("matches")
                    .child(_match.value!!.id).removeValue()
            }
        }
        //now delete the match
        databaseRef.child(path).child(matchId).removeValue()

        //now we recreate the match, but with an inverted privacy status
        var newPath: String?
        if(oldStatus){
            newPath = "privateMatches"
            _match.value!!.isPublic = false
        } else {
            newPath = "publicMatches"
            _match.value!!.isPublic = true
        }
        val id = databaseRef.child(newPath).push().key
        _match.value!!.id = id!!
        val matchValues = _match.value!!.toMap()
        val childUpdates = HashMap<String, Any>()

        //put into database
        childUpdates["/$newPath/$id"] = matchValues
        databaseRef.updateChildren(childUpdates)

        //now update for all the participants
        for(entry in _match.value!!.participants!!){
            val uid = entry.key
            val userUpdates = HashMap<String, Any>()
            userUpdates["/users/$uid/matches/$id"] = matchValues
            databaseRef.updateChildren(userUpdates)
        }
        //now update all the invited ones
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            for (entry in match.value!!.pendingList!!) {
                val uid = entry.key
                val userUpdates = HashMap<String, Any>()
                userUpdates["/users/$uid/matches/$id"] = matchValues
                databaseRef.updateChildren(userUpdates)
            }
        }

        Toast.makeText(
            this@MatchViewModel.getApplication(),
            "Match Privacy updated successfully!",
            Toast.LENGTH_LONG
        ).show()

        //now to the updated match!
        this.path = newPath
        this.matchId = id
        getMatchById()
    }

    fun addParticipant(){
        if(_match.value!!.numberParticipants!! <_match.value!!.max_number!!) {
            val newValue = _match.value!!.numberParticipants?.plus(1)
            _match.value!!.numberParticipants = newValue
            val matchValues = _match.value!!.toMap()
            val childUpdates = HashMap<String, Any>()
            //put into database
            childUpdates["/$path/$matchId"] = matchValues
            databaseRef.updateChildren(childUpdates)
            //update every reference of the participants
            for(entry in _match.value!!.participants!!){
                val uid = entry.key
                databaseRef.child("users").child(uid).child("matches")
                    .child(matchId).child("numberParticipants").setValue(newValue)
            }
            //update every reference of the invited ones
            if(!match.value!!.pendingList.isNullOrEmpty()){
                for(entry in _match.value!!.pendingList!!){
                    val i_uid = entry.key
                    databaseRef.child("users").child(i_uid).child("matches")
                        .child(matchId).child("numberParticipants").setValue(newValue)
                }
            }
            if(match.value!!.numberParticipants!! == match.value!!.max_number!!){
                autoClean()
            }
        }
    }

    fun removeParticipant(){
        if(_match.value!!.numberParticipants!! > match.value!!.participants!!.size) {
            val newValue = _match.value!!.numberParticipants?.minus(1)
            _match.value!!.numberParticipants = newValue
            val matchValues = _match.value!!.toMap()
            val childUpdates = HashMap<String, Any>()
            //put into database
            childUpdates["/$path/$matchId"] = matchValues
            databaseRef.updateChildren(childUpdates)
            //update every reference of the participants
            for(entry in _match.value!!.participants!!){
                val uid = entry.key
                databaseRef.child("users").child(uid).child("matches")
                    .child(_match.value!!.id!!).child("numberParticipants").setValue(newValue)
            }
            //update every reference of the invited ones
            if(!match.value!!.pendingList.isNullOrEmpty()){
                for(entry in _match.value!!.pendingList!!){
                    var i_uid = entry.key
                    databaseRef.child("users").child(i_uid).child("matches")
                        .child(matchId).child("numberParticipants").setValue(newValue)
                }
            }
        }
    }

    fun inviteFriends(){
        _navigateToFriends.value = "FRIENDS"
    }

    fun showPlayers(){
        _navigateToPlayers.value = "PLAYERS"
    }

    fun showRequests(){
        _navigateToRequests.value = "REQUESTS"
    }

    fun archiveMatch(){
        if(match.value!!.isConfirmed!! && (today > match.value!!.date) && myself.uid == match.value!!.ownerId){
            var matchValues = match.value!!.toMap()
            for(entry in match.value!!.participants!!){
                var uid = entry.key
                var matchId = match.value!!.id
                var childUpdates = HashMap<String, Any>()
                childUpdates["/archivedMatches/$uid/$matchId"] = matchValues
                databaseRef.updateChildren(childUpdates)
            }
            Toast.makeText(this@MatchViewModel.getApplication(), "Match archived successfully.",
                Toast.LENGTH_LONG).show()
            deleteMatch()
        }
    }

    fun requestParticipation(){
        //if the event is not full
        if(match.value!!.numberParticipants!! < match.value!!.max_number!!) {
            coroutineScope.launch {
                val user = database.getByUid(uid)
                val userValues = user.toMap()
                val userUpdates = HashMap<String, Any>()
                //update global
                userUpdates["/$path/$matchId/pendingRequests/$uid"] = userValues
                databaseRef.updateChildren(userUpdates)
                //update owner
                val ownerUpdates = HashMap<String, Any>()
                ownerUpdates.put(
                    "/users/" + _match.value!!.ownerId + "/matches/" + matchId + "/pendingRequests/" + uid,
                    userValues
                )
                databaseRef.updateChildren(ownerUpdates)
            }
            _matchUserStatus.value = MatchUserStatus.REQUESTING
        }
    }

    fun acceptInvitation(){
        //if the event is not full
        if(match.value!!.numberParticipants!! < match.value!!.max_number!!) {
            //update count
            addParticipant()
            //first remove user from pending list
            databaseRef.child(path).child(matchId).child("pendingList").child(uid).removeValue()
            //do the same for the owner
            databaseRef.child("users").child(match.value!!.ownerId!!).child("matches")
                .child(matchId).child("pendingList").child(uid).removeValue()
            //now add to the list of participants
            coroutineScope.launch {
                val user = database.getByUid(uid)
                val userValues = user.toMap()
                val userUpdates = HashMap<String, Any>()
                //update global
                userUpdates["/$path/$matchId/participants/$uid"] = userValues
                databaseRef.updateChildren(userUpdates)
                //update for every participant
                for(entry in _match.value!!.participants!!){
                    val p_uid = entry.key
                    val participantsUpdates = HashMap<String, Any>()
                    participantsUpdates["/users/$p_uid/matches/$matchId/participants/$uid"] =
                        userValues
                    databaseRef.updateChildren(participantsUpdates)
                }
                //update for every invited one
                if(!match.value!!.pendingList.isNullOrEmpty()) {
                    for (entry in _match.value!!.pendingList!!) {
                        val i_uid = entry.key
                        var invitedUpdates = HashMap<String, Any>()
                        invitedUpdates["/users/$i_uid/matches/$matchId/participants/$uid"] =
                            userValues
                        databaseRef.updateChildren(invitedUpdates)
                    }
                }
            }
            //update UI
            _matchUserStatus.value = MatchUserStatus.PARTICIPANT
            //autoclean if max number
            if(match.value!!.numberParticipants!! == match.value!!.max_number!!){
                autoClean()
            }
        }
    }

    fun denyInvitation(){
        //first remove user from pending list
        databaseRef.child(path).child(matchId).child("pendingList").child(uid).removeValue()
        //do the same for the owner
        databaseRef.child("users").child(match.value!!.ownerId!!).child("matches")
            .child(matchId).child("pendingList").child(uid).removeValue()
        //update global reference to match with the refuser's id
        databaseRef.child(path).child(matchId).child("refused").child(uid).setValue(myself.displayName)
        //last, remove match reference from user denying invitation
        databaseRef.child("users").child(uid).child("matches").child(matchId).
                removeValue()
    }

    fun leaveMatch(){
        //update count
        var newValue = _match.value!!.numberParticipants?.minus(1)
        _match.value!!.numberParticipants = newValue
        val matchValues = _match.value!!.toMap()
        val childUpdates = HashMap<String, Any>()
        //put into database
        childUpdates.put("/"+path+"/"+matchId, matchValues)
        databaseRef.updateChildren(childUpdates)
        //update every reference of the participants
        for(entry in _match.value!!.participants!!){
            var uid = entry.key
            databaseRef.child("users").child(uid).child("matches")
                .child(_match.value!!.id!!).child("numberParticipants").setValue(newValue)
        }
        //update every reference of the invited ones
        if(!match.value!!.pendingList.isNullOrEmpty()){
            for(entry in _match.value!!.pendingList!!){
                var i_uid = entry.key
                databaseRef.child("users").child(i_uid).child("matches")
                    .child(matchId).child("numberParticipants").setValue(newValue)
            }
        }
        //remove user from participants (global)
        databaseRef.child(path).child(matchId).child("participants").child(uid).removeValue()
        //remove user from participants for every participant
        for(entry in _match.value!!.participants!!){
            var p_uid = entry.key
            databaseRef.child("users").child(p_uid).child("matches").child(matchId).
                    child("participants").child(uid).removeValue()
        }
        //same as before for every invited one
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            for (entry in _match.value!!.pendingList!!) {
                var i_uid = entry.key
                databaseRef.child("users").child(i_uid).child("matches").child(matchId).
                    child("participants").child(uid).removeValue()
            }
        }
        //now remove user's reference to the match
        databaseRef.child("users").child(uid).child("matches").child(matchId).
                removeValue()
        //tell everyone you're gone
        databaseRef.child(path).child(matchId).child("removed").child(uid).setValue(uid)
        //update UI
        _matchUserStatus.value = MatchUserStatus.NONE
    }

    fun autoClean(){
        //when max number of players has been reached after interaction, automatically
        //clean every request/invite for the match - a notification will be sent
        //clean pendingList
        if(!match.value!!.pendingList.isNullOrEmpty()) {
            //this should be necessary to send single notification to everyone
            for (entry in _match.value!!.pendingList!!) {
                var i_uid = entry.key
                //remove reference of the invited ones
                databaseRef.child("users").child(i_uid).child("matches").child(matchId).
                    removeValue()
                //update global reference to match with the rejected id
                databaseRef.child(path).child(matchId).child("rejected").child(i_uid).setValue(i_uid)
            }
            databaseRef.child("users").child(match.value!!.ownerId!!).child("matches").
                child(matchId).child("pendingList").removeValue()
        }
        //clean requestsList
        if(!match.value!!.pendingRequests.isNullOrEmpty()) {
            //this should be necessary to send single notification to everyone
            for (entry in _match.value!!.pendingRequests!!) {
                var p_uid = entry.key
                //update global reference to match with the rejected id
                databaseRef.child(path).child(matchId).child("rejected").child(p_uid).setValue(p_uid)
            }
            databaseRef.child("users").child(match.value!!.ownerId!!).child("matches").
                child(matchId).child("pendingRequests").removeValue()
        }
    }

    fun onNavigatedBackToHome(){
        _navigateBackToHome.value = null
    }

    fun onNavigatedFriends(){
        _navigateToFriends.value = null
    }

    fun onNavigatedPlayers(){
        _navigateToPlayers.value = null
    }

    fun onNavigatedRequests(){
        _navigateToRequests.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun checkConnection(){
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    //already defined
                } else {
                    _fragmentStatus.value = MatchFragmentStatus.NETWORK_ERROR
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CONNECTION", "Listener was cancelled")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        //needed to create map
        map = googleMap
    }

    fun updateMapLocation(map: GoogleMap){
        val zoomLevel = 15f
        //var overlaySize = 80f
        val homeLatLng = LatLng(
            match.value!!.placeLatLng!!.split(",")[0].toDouble(),
        match.value!!.placeLatLng!!.split(",")[1].toDouble())

        map.addMarker(MarkerOptions().position(homeLatLng).title(match.value!!.place))
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

        setMarkerClickToGmaps(map, match.value!!.place)

        /*Overlay image on match marker on map
        //Set Match icon on HomeLatLng
        val googleOverlay = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.match_marker))
            .position(homeLatLng, overlaySize)
            Log.i(TAG, "oversize 2: $overlaySize")
        map.addGroundOverlay(googleOverlay)*/

        /*  attempt to edit overlay size of groundOverlay image by catching user zoom level to update
            overlaySize value */
        /*map.setOnCameraChangeListener(object: GoogleMap.OnCameraChangeListener {
            private var currentZoom = -1f
            override fun onCameraChange(pos: CameraPosition) {
                if (pos.zoom != currentZoom)
                {
                    currentZoom = pos.zoom
                    Log.i(TAG, "zoom: $currentZoom")
                    overlaySize *= 1/(currentZoom)
                    Log.i(TAG, "oversize 1: $overlaySize")

                }
            }
        })*/
    }

    private fun setMarkerClickToGmaps(map: GoogleMap, myAddress: String){
        map.setOnMarkerClickListener{
            /* Intent to search for a Location:
                geo:latitude,longitude?q=query
                geo:0,0?q=my+street+address
                geo:0,0?q=latitude,longitude(label)
            */
            val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=" + Uri.encode(myAddress))
            val mapIntentTemp = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntentTemp.setPackage("com.google.android.apps.maps")
            _mapIntent.value = mapIntentTemp
            _navigateToMaps.call()
            true
        }
    }

    fun mapSet(){
        _isMapReady.value = null
    }

}
