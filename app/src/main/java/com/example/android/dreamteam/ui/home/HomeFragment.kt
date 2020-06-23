package com.example.android.dreamteam.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.MatchDatabase
import com.example.android.dreamteam.database.UserDatabase
import com.example.android.dreamteam.databinding.FragmentHomeBinding
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.utils.HomePageAdapter
import com.google.android.gms.location.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val TOPIC_FRIENDS = FirebaseAuth.getInstance().currentUser!!.uid+"_friends"
private val TOPIC_MATCHES = FirebaseAuth.getInstance().currentUser!!.uid+"_matches"

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var homePageAdapter: HomePageAdapter
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val databaseRef = Firebase.database.reference
    private val myself = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentHomeBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_home, container, false
        )
        // notify on creation that options menu is available here
        setHasOptionsMenu(true)
        val application = requireNotNull(this.activity).application
        val dataSource = MatchDatabase.getInstance(application).matchDatabaseDao
        val viewModelFactory = HomeViewModelFactory(dataSource, application)
        homeViewModel = ViewModelProvider(
            this, viewModelFactory
        ).get(HomeViewModel::class.java)

        val adapter = MatchAdapter(MatchListener { matchId, isPublic ->
            homeViewModel.onMatchClicked(matchId, isPublic)
        })

        binding.lifecycleOwner = this
        binding.homeViewModel = homeViewModel
        binding.searchMatches.adapter = adapter
        homeViewModel.search.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })


        binding.fab.setOnClickListener { view ->
            view.findNavController().navigate(R.id.action_navigation_home_to_newMatchFragment2)
        }

        // listener to click on Match + destroy options menu
        homeViewModel.navigateToMatchFragment.observe(viewLifecycleOwner, Observer { match ->
            match?.let {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToMatchFragment(
                        match,
                        homeViewModel.path.value!!
                    )
                )
                onDestroyOptionsMenu()
                homeViewModel.onMatchNavigated()
            }
        })

        getUser()
        subscribeTopics()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        homePageAdapter = HomePageAdapter(this)
        viewPager = view.findViewById(R.id.pager_home)
        viewPager.adapter = homePageAdapter
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout_home)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if(position==0) tab.text = "Public Matches"
            else tab.text = "My Matches"
        }.attach()
    }

    // create the options menu = create the search bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.options_menu, menu)
        val item: MenuItem = menu.findItem(R.id.search)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        // now handle the search, if any is present
        val searchView: SearchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                //this is where the query submitted by the user is processed
                homeViewModel.getMatchesByName(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // extra: this can be used to dynamically change view when querying
                return true
            }
        })

        searchView.setOnCloseListener(object : SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                homeViewModel.closeSearch()
                return true
            }
        })
    }


    // this is to handle correctly the back button of the search View!
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            homeViewModel.closeSearch()
            return true
        }
        return false
    }

    //this function is called only at first launch, it retrieves user's data from Firebase
    //and puts it into local Room db
    private fun getUser(){
        val application = requireNotNull(this.activity).application
        val database = UserDatabase.getInstance(application).userDatabaseDao
        coroutineScope.launch {
            var userRetrieved: User? = database.getByUid(myself)
            if (userRetrieved == null) { //first launch
                var userFirebase = databaseRef.child("users").child(myself)
                val userListener = object : ValueEventListener {

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userSnapshot: Map<String, Object> = dataSnapshot.value as Map<String, Object>
                        val user = User().fromMap(userSnapshot)
                        coroutineScope.launch {
                            database.insert(user)
                        }
                        }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("FAIL ACCOUNT", "FAIL:" + databaseError.message)
                    }

                }

                userFirebase.addListenerForSingleValueEvent(userListener)
            }
        }
    }

    //subscribe to topics for notifications and create a channel for APIs>26
    //is called only at first launch and only if user has set notifications preference to true
    private fun subscribeTopics() {
        val application = requireNotNull(this.activity).application
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        val database = UserDatabase.getInstance(application).userDatabaseDao
        coroutineScope.launch {
            val isUserIn = database.verifyByUid(myself)
            if (isUserIn != null) {
                //already subscribed, nothing to do here
                Log.w("USER:", isUserIn.toString())
            }
            else if(sharedPreferences.getBoolean("notifications", true)){
                createChannel()
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_FRIENDS)
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_MATCHES)
            }
        }
    }

    private fun createChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(getString(R.string.channel_ID),
                getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableVibration(true)
            notificationChannel.enableLights(true)
            val notificationManager = requireActivity().getSystemService(
                NotificationManager::class.java
            )
            if(notificationManager.notificationChannels.isEmpty())
                notificationManager.createNotificationChannel(notificationChannel)
        }
    }


}
