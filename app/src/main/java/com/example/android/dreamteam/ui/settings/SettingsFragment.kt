package com.example.android.dreamteam.ui.settings

import android.app.Application
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth
import com.example.android.dreamteam.LoginActivity
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.UserDatabase
import com.example.android.dreamteam.database.UserDatabaseDao
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*

private const val EMAIL = "emailPublic"
private const val PHONE = "phonePublic"
private const val NOTIFICATIONS = "notifications"

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var application: Application
    private lateinit var database: UserDatabaseDao
    private val auth = FirebaseAuth.getInstance()
    private val myself = auth.currentUser!!
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    //handle network
    private var databaseRef: DatabaseReference = Firebase.database.reference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        application = requireNotNull(this.activity).application
        database = UserDatabase.getInstance(application).userDatabaseDao
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val logoutPreference: Preference? = findPreference("logout")
        setLogout(logoutPreference)
        val accountPreference: Preference? = findPreference("account")
        setAccount(accountPreference)
        val archivePreference: Preference? = findPreference("archive")
        setArchive(archivePreference)

    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key == EMAIL || key == PHONE || key == NOTIFICATIONS) {
            val flag = sharedPreferences!!.getBoolean(key, false)
            when (key) {
                EMAIL -> {
                    handlePref(myself.uid, EMAIL, flag)
                }
                PHONE -> {
                    handlePref(myself.uid, PHONE, flag)
                }
                NOTIFICATIONS -> {
                    handlePref(myself.uid, NOTIFICATIONS, flag)
                }
            }
        }
    }

    private fun handlePref(uid: String, preference: String, flag: Boolean){
        if(preference == NOTIFICATIONS){
            if(flag == false) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(uid + "_friends")
                FirebaseMessaging.getInstance().unsubscribeFromTopic(uid + "_matches")
            } else {
                FirebaseMessaging.getInstance().subscribeToTopic(uid + "_friends")
                FirebaseMessaging.getInstance().subscribeToTopic(uid + "_matches")
            }
        }
        else {
            coroutineScope.launch {
                var userRef = databaseRef.child("users").child(myself.uid)
                if (preference == EMAIL) userRef.child("emailPublic").setValue(flag)
                if (preference == PHONE) userRef.child("phonePublic").setValue(flag)
                if (database.isUserIn() > 0) {
                    var user = database.getByUid(myself.uid)
                    if (preference == EMAIL) user.emailPublic = flag
                    if (preference == PHONE) user.phonePublic = flag
                    database.update(user)
                }
            }
        }
        Toast.makeText(
            getActivity()!!.applicationContext,
            "Updated succesfully.",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun setLogout(logout: Preference?){
        logout?.setOnPreferenceClickListener {
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            Toast.makeText(this.context, "Logout riuscito.", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            getActivity()?.finish()
            true
        }

    }

    private fun setAccount(account: Preference?){
        account?.setOnPreferenceClickListener {
            var bundle = bundleOf("uid" to myself.uid)
            view?.findNavController()?.navigate(R.id.action_navigation_settings_to_accountFragment, bundle)
            true
        }
    }

    private fun setArchive(archive: Preference?){
        archive?.setOnPreferenceClickListener {
            var bundle = bundleOf("WHICH_LIST" to 3)
            view?.findNavController()?.navigate(R.id.action_navigation_settings_to_matchListFragment, bundle)
            true
        }
    }

    override fun onResume(){
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause(){
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelJob.cancel()
    }

}
