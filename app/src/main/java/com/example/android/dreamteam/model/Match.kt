package com.example.android.dreamteam.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset


@Entity(tableName = "match_table")
@IgnoreExtraProperties
data class Match (
    @PrimaryKey(autoGenerate = true)
    var matchId: Long = 0L,
    @ColumnInfo
    var id: String = "",
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    var date: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    @ColumnInfo
    var place: String = "",
    @ColumnInfo
    var placeLatLng: String? = "",
    @ColumnInfo
    var ownerId: String? = "",
    @ColumnInfo
    var isPublic: Boolean? = false,
    @ColumnInfo
    var type: String? = "",
    @ColumnInfo
    var max_number: Long? = 0,
    @Ignore
    var participants: Map<String, Object>? = hashMapOf(),
    @ColumnInfo
    var numberParticipants: Long? = 1,
    @ColumnInfo
    var price: Double? = 0.0,
    @Ignore
    var pendingList: Map<String, Object>? = hashMapOf(),
    @Ignore
    var pendingRequests: Map<String, Object>? = hashMapOf(),
    @Ignore
    var rejected: Map<String, Object>? = hashMapOf(),
    @Ignore
    var refused: Map<String, Object>? = hashMapOf(),
    @Ignore
    var removed: Map<String, Object>? = hashMapOf(),
    @Ignore
    var isConfirmed: Boolean? = true
) {

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "matchId" to matchId,
            "id" to id,
            "name" to name,
            "date" to date,
            "place" to place,
            "placeLatLng" to placeLatLng,
            "ownerId" to ownerId,
            "isPublic" to isPublic,
            "type" to type,
            "max_number" to max_number,
            "participants" to participants,
            "numberParticipants" to numberParticipants,
            "price" to price,
            "pendingList" to pendingList,
            "pendingRequests" to pendingRequests,
            "rejected" to rejected,
            "refused" to refused,
            "removed" to removed,
            "isConfirmed" to isConfirmed
        )
    }

    @Exclude
    fun fromMap(map: Map<String, Object>): Match {
        var priceDouble: Double? = map["price"].toString().toDouble()
        return Match(map["matchId"] as Long, map["id"] as String, map["name"] as String, map["date"] as Long,
            map["place"] as String, map["placeLatLng"] as String?, map["ownerId"] as String?, map["isPublic"] as Boolean?, map["type"] as String?,
            map["max_number"] as Long?, map["participants"] as Map<String, Object>?, map["numberParticipants"] as Long?,
            priceDouble,
            map["pendingList"] as Map<String, Object>?, map["pendingRequests"] as Map<String, Object>?,
            map["rejected"] as Map<String, Object>?, map["refused"] as Map<String, Object>?,
            map["removed"] as Map<String, Object>?, map["isConfirmed"] as Boolean?)
    }

}




