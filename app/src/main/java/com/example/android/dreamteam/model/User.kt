package com.example.android.dreamteam.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Entity(tableName = "user_table")
@IgnoreExtraProperties
data class User (
    @PrimaryKey(autoGenerate = true)
    var userId: Long = 0L,
    @ColumnInfo
    var uid: String? = "",
    @ColumnInfo
    var email: String? = "",
    @ColumnInfo
    var name: String? = "",
    @ColumnInfo
    var picture: String? = "",
    @ColumnInfo
    var rating: Double? = 0.0,
    @ColumnInfo
    var phoneNumber: String? = "",
    @ColumnInfo
    var role: String? = "None",
    @ColumnInfo
    var info: String? = "",
    @ColumnInfo
    var friend: Boolean? = false,
    @ColumnInfo
    var emailPublic: Boolean? = true,
    @ColumnInfo
    var phonePublic: Boolean? = false,
    @Ignore
    var friendsList: Map<String, Object>? = hashMapOf(),
    @Ignore
    var pendingRequests: Map<String, Object>? = hashMapOf(),
    @Ignore
    var matches: Map<String, Object>? = hashMapOf()
) {
    val is_f
        get() = friend == true

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "picture" to picture,
            "rating" to rating,
            "phone_number" to phoneNumber,
            "role" to role,
            "info" to info,
            "emailPublic" to emailPublic,
            "phonePublic" to phonePublic,
            "friendsList" to friendsList,
            "pendingRequests" to pendingRequests,
            "matches" to matches
        )
    }

    @Exclude
    fun fromMap(map: Map<String, Object>): User{
        return User(0L, map["uid"] as String, map["email"] as String?, map["name"] as String,
            map["picture"] as String,
            map["rating"] as Double?, map["phone_number"] as String?,
            map["role"] as String?, map["info"] as String?,
            false, map["emailPublic"] as Boolean?,
            map["phonePublic"] as Boolean?,
            map["friendsList"] as Map<String, Object>?,
            map["pendingRequests"] as Map<String, Object>?, map["matches"] as Map<String, Object>?)
    }

}