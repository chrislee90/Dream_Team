package com.example.android.dreamteam.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.MatchListFragmentStatus
import com.example.android.dreamteam.utils.UserApiStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import kotlin.math.*

private const val MAX_RANGE = 30 //only report matches in 30 km

class MatchListViewModel(private val which: Int) : ViewModel() {

    val _matches = MutableLiveData<MutableList<Match>>()
    val matches: LiveData<MutableList<Match>>
        get() = _matches

    val _position = MutableLiveData<Double>()
    val position: LiveData<Double>
        get() = _position

    val _navigateToMatchDetail = MutableLiveData<String>()
    val navigateToMatchDetail: LiveData<String>
        get() = _navigateToMatchDetail

    val _status = MutableLiveData<UserApiStatus>()
    val status: LiveData<UserApiStatus>
        get() = _status

    val _fragmentStatus = MutableLiveData<MatchListFragmentStatus>()
    val fragmentStatus: LiveData<MatchListFragmentStatus>
        get() = _fragmentStatus

    val _path = MutableLiveData<String>()
    val path: LiveData<String>
        get() = _path

    val today = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toDouble()

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference


    //start loading matches at today's date, load only the first <limit> matches
    //also, if latitude and longitude of the user are not null (= the location is on)
    //only the matches in a distance <= MAX_RANGE will be displayed
    fun getPublicMatches(limit: Int, latitude: Double?, longitude: Double?, refresh: Boolean){
        var matchesReference = databaseRef.child("publicMatches")
        var publicMatches = matchesReference.orderByChild("date").startAt(today)

        val matchListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var result = mutableListOf<Match>()
                _matches.value = result
                if(!dataSnapshot.hasChildren()){
                    _fragmentStatus.value = MatchListFragmentStatus.NO_MATCHES
                    _status.value = UserApiStatus.DONE
                }
                else{
                    loop@ for(snapshot in dataSnapshot.children){
                        var match = Match().fromMap(snapshot.value as Map<String, Object>)
                        if(latitude!=null && longitude!= null) {
                            if (isInRange(match, latitude, longitude))
                                result.add(match)
                        }
                        else result.add(match)
                        _position.value = match.date.toDouble()
                        if(result.size == limit) break@loop // break when you reach a value of <limit> matches to display
                    }
                    _matches.value = result
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = MatchListFragmentStatus.DEFAULT
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }

        }

        publicMatches.addValueEventListener(matchListener)
        if(!refresh) checkConnection()
    }

    fun loadOtherPublicMatches(adapter: MatchAdapter, latitude: Double?, longitude: Double?){
        val limit = 10
        var matchesReference = databaseRef.child("publicMatches")
        var publicMatches = matchesReference.orderByChild("date").startAt(_position.value!!+1)

        val matchListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var count = 0
                if(!dataSnapshot.hasChildren()){
                    // do nothing, there is no other match to fetch
                }
                else{
                    loop@ for(snapshot in dataSnapshot.children){
                        var match = Match().fromMap(snapshot.value as Map<String, Object>)
                        if(latitude!=null && longitude!=null) {
                            if (isInRange(match, latitude, longitude)){
                                _matches.value!!.add(match)
                                count+=1
                            }
                        }
                        else{
                            _matches.value!!.add(match)
                            count+=1
                        }
                        _position.value = match.date.toDouble()
                        if(count == limit) break@loop
                    }
                    adapter.notifyDataSetChanged()
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = MatchListFragmentStatus.DEFAULT
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }

        }
        _fragmentStatus.value = MatchListFragmentStatus.LOADING
        publicMatches.addListenerForSingleValueEvent(matchListener)
    }

    fun getMyMatches(uid: String, refresh: Boolean){
        var userReference = databaseRef.child("users").child(uid)

        val matchListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var result = mutableListOf<Match>()
                val userSnapshot = dataSnapshot.value as Map<String, Object>
                val user = User().fromMap(userSnapshot)
                _matches.value = result
                var myMatches = user.matches
                if(myMatches.isNullOrEmpty()){
                    _fragmentStatus.value = MatchListFragmentStatus.NO_MATCHES
                    _status.value = UserApiStatus.DONE
                }
                else{
                    for(entry in myMatches){
                        var match = Match().fromMap(entry.value as Map<String, Object>)
                        result.add(match)
                    }
                    result.sortBy { match -> match.date }
                    _matches.value = result
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = MatchListFragmentStatus.DEFAULT
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }
        }

        userReference.addListenerForSingleValueEvent(matchListener)
        if(!refresh) checkConnection()
    }

    // load archived matches
    fun getMyArchivedMatches(uid: String, refresh: Boolean){
        var archivedReference = databaseRef.child("archivedMatches").child(uid)

        val matchListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var result = mutableListOf<Match>()
                _matches.value = result
                if(!dataSnapshot.hasChildren()){
                    _fragmentStatus.value = MatchListFragmentStatus.NO_MATCHES
                    _status.value = UserApiStatus.DONE
                }
                else{
                    for(snapshot in dataSnapshot.children){
                        var match = Match().fromMap(snapshot.value as Map<String, Object>)
                        result.add(match)
                    }
                    _matches.value = result
                    _status.value = UserApiStatus.DONE
                    _fragmentStatus.value = MatchListFragmentStatus.DEFAULT
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FAIL MATCH", "FAIL:" + databaseError.message)
            }
        }

        archivedReference.addListenerForSingleValueEvent(matchListener)
        if(!refresh) checkConnection()
    }

    // check connection with Firebase

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
        _fragmentStatus.value = MatchListFragmentStatus.ERROR
    }

    fun onMatchClicked(matchId: String, isPublic: Boolean){
        if(isPublic) _path.value = "publicMatches"
        else _path.value = "privateMatches"
        _navigateToMatchDetail.value = matchId
    }

    fun onMatchNavigated(){
        _navigateToMatchDetail.value = null
        _path.value = null
    }

    //this function checks whether a given match is in user's location range (HAVERSINE DISTANCE)
    fun isInRange(match: Match, latitude: Double, longitude: Double): Boolean{
        var match_lat = match.placeLatLng!!.split(",")[0].toDouble()
        var match_lon = match.placeLatLng!!.split(",")[1].toDouble()
        var R = 6371; // Radius of the earth in km
        var dLat = deg2rad(latitude-match_lat)  // deg2rad below
        var dLon = deg2rad(longitude-match_lon)
        var a =
            sin(dLat/2) * sin(dLat/2) +
                    cos(deg2rad(match_lat)) * cos(deg2rad(latitude)) *
                    sin(dLon/2) * sin(dLon/2)
        ;
        var c = 2 * atan2(sqrt(a), sqrt(1-a));
        var d = R * c; // Distance in km

        //now the check
        return d <= MAX_RANGE
    }

    fun deg2rad(deg: Double): Double {
        return (deg * (Math.PI/180)) //there should be no absoulteValue according to the formula (HAVERSINE DISTANCE)
    }

}
