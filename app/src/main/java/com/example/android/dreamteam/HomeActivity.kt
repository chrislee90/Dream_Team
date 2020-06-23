package com.example.android.dreamteam

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jakewharton.threetenabp.AndroidThreeTen


class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_settings, R.id.navigation_notifications, R.id.navigation_friends
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // if there is an intent when calling an activity, it means it is opening from a notification
        // so navigate to the notifications fragment
        val uid = intent.extras?.get("uid")
        if(uid!=null){
            Log.w("TARGET:", uid.toString())
            navView.selectedItemId = R.id.navigation_notifications
        }

        AndroidThreeTen.init(this)

        //Google places initialized in the Home activity--------------------------------
        val apiKey = getString(R.string.places_api_key)
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_api_key), Toast.LENGTH_LONG).show()
            return
        }

        // Setup Places Client
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        //END of Google Places initialization in Home Activity-----------------------------------
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
