package com.example.android.dreamteam.ui.chat

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

data class Message (
    var message :String = "",
    var sender :String = "",
    var mine: Boolean = false,
    var timestamp :Long = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
)