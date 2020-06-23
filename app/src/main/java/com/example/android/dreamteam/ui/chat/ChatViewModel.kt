package com.example.android.dreamteam.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.example.android.dreamteam.chat.XMPPChat
import org.jivesoftware.smack.chat2.ChatManager


class ChatViewModel : ViewModel() {

    var message = MutableLiveData<String>()
    val messageList = XMPPChat.get().messageList

    init {

    }

    fun sendMessage(){
        XMPPChat.get().sendMessage(message.value ?: "")
        message.value = ""
    }

    fun startChat(context: Context, matchId : String) {
        XMPPChat.get().connect(context, matchId, false)
    }

    fun closeChat(){
        try {
            XMPPChat.get().logout()
        } catch(e: Exception){
            //do nothing, go back anyways
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeChat()
    }

}
