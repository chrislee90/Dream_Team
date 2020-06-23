package com.example.android.dreamteam.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDateTime

@Entity(tableName="notification_table")
data class Notification (
    @PrimaryKey(autoGenerate=true)
    var id: Int? = 0,
    @ColumnInfo
    var message: String? = "",
    @ColumnInfo
    var pictureUrl: String? = "",
    @ColumnInfo
    var path: String? = "",
    @ColumnInfo
    var target: String? = "",
    @ColumnInfo
    var date: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo
    var checked: Boolean = false,
    @ColumnInfo
    var uid: String? = ""
) {
    val is_c
        get() = checked == true
}