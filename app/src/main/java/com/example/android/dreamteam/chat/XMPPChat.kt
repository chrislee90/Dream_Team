package com.example.android.dreamteam.chat


import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.android.dreamteam.ui.chat.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.MessageListener
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.iqregister.AccountManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Domainpart
import org.jxmpp.jid.parts.Localpart
import org.jxmpp.jid.parts.Resourcepart
import java.net.InetAddress


class XMPPChat {

    //server specific
    private val DOMAIN = "ec2-54-209-7-127.compute-1.amazonaws.com"
    private val PORT = 5222
    private val HOST_ADDRESS = "54.209.7.127"

    var connection: XMPPTCPConnection
    private var registered = false

    val messageList = MutableLiveData<ArrayList<Message>>(ArrayList())

    private var username : String
    private var email : String
    lateinit var chat: MultiUserChat

    private var chatJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + chatJob)

    companion object {
        @Volatile
        private var singleton: XMPPChat? = null

        fun get(): XMPPChat =
            singleton ?: synchronized(this) {
                singleton ?: XMPPChat().also { singleton = it }
            }
    }

    init {
        val hostAddress = InetAddress.getByName(HOST_ADDRESS)

        connection = XMPPTCPConnection(
            XMPPTCPConnectionConfiguration.builder()
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setPort(PORT)
                .setXmppDomain(DOMAIN)
                .enableDefaultDebugger()
//                .setConnectTimeout(5000000)
                .setSendPresence(true)
//                .setCompressionEnabled(true)
//                .setKeystoreType(null)
                .setHostAddress(hostAddress)
                .build()
        )

//        setMessageReceiver()

        email = FirebaseAuth.getInstance().currentUser?.email!!.replace('@', '_')
        username = FirebaseAuth.getInstance().currentUser?.displayName!!
    }

    fun connect(context: Context, matchId: String, creating: Boolean) {
        uiScope.launch {
            connectAndLogin(matchId, creating)
        }
        addUserToMembersGroup(context, matchId)
    }

    fun logout() {
        messageList.value = ArrayList()
        chat.removeMessageListener(messageListener)
        uiScope.launch {
            disconnect()
        }
    }

    fun sendMessage(message: String) {
        uiScope.launch {
            onSend(message)
        }
    }

    fun addUserToMembersGroup(context: Context, matchId: String){
        uiScope.launch {
            userToMember(context, matchId)
        }
    }

    private suspend fun connectAndLogin(matchId: String, creating: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (connection.isConnected) {
                    if (connection.isAuthenticated)
                        Log.d("chat", "already logged in")
                    else {
                        createAccount()
                        connection.login(email, email)
                        Log.d("chat", "logged in")
                    }
                } else {
                    connection.connect()
                    Log.d("chat", "connected to server")
                    createAccount()
                    connection.login(email, email)
                    Log.d("chat", "logged in")
                }

                connection.sendStanza(Presence(Presence.Type.available))

                joinRoom(matchId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                chat.leave()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private suspend fun onSend(message: String) {
        withContext(Dispatchers.IO) {
            if (message.isNotEmpty())
                chat.sendMessage(message)
        }
    }

    private fun createAccount(): Boolean {
        val account = AccountManager.getInstance(connection)

        try {
            if (account.supportsAccountCreation()) {
                account.sensitiveOperationOverInsecureConnection(true)
                account.createAccount(Localpart.from(email), email)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return false
    }

    private suspend fun userToMember(context : Context, matchId: String){
        // Registra utente nel gruppo membri
        withContext(Dispatchers.IO){
            val queue = Volley.newRequestQueue(context)
            val url = "http://$HOST_ADDRESS:9090/plugins/restapi/v1/chatrooms/${matchId.toLowerCase()}/owners/$email"
            Log.w("chat url rest", url)
            val stringRequest = object : StringRequest(Request.Method.POST, url,
                Response.Listener<String> { response ->
                    Log.e("chat GROUP","Response is: ${response ?: ""}")
                },
                Response.ErrorListener {
                    Log.e("chat GROUP", "ERROR ADDING USER TO GROUP MEMBERS: $it")
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "DreamTeam"
                    return headers
                }
            }
            queue.add(stringRequest)
            queue.start()
        }
    }

    private fun joinRoom(matchId: String) {
        val xmppServiceGroupDomain: Domainpart = Domainpart.from("conference.$DOMAIN")
        var mucJid = JidCreate.entityBareFrom(Localpart.from(matchId), xmppServiceGroupDomain)

        val manager = MultiUserChatManager.getInstanceFor(connection)
        chat = manager.getMultiUserChat(mucJid)

        chat.addMessageListener(messageListener)

        val mucEnterConfiguration = chat.getEnterConfigurationBuilder(Resourcepart.from(username))
//            .withPresence(Presence(Presence.Type.available))
            .withPassword("password")
            .build()

        if (!chat.isJoined)
            chat.join(mucEnterConfiguration)
        if (chat.isJoined) Log.e("Sei connesso", "alla room")

    }

    private val messageListener = MessageListener {
        var from = it.from.toString()

        var contactJid = ""
        if (from.contains("/")) {
            contactJid = from.split("/")[1];
        } else {
            contactJid = from;
        }

        if (it.body != null)
            updateMessageList(Message(it.body, contactJid, contactJid == username))
    }

    private fun updateMessageList(message: Message) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                var updatedList = ArrayList<Message>(messageList.value!!)
                updatedList.add(message)
                messageList.value = updatedList
            }
        }
    }
}