package com.example.android.dreamteam.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.android.dreamteam.R
import com.example.android.dreamteam.databinding.MatchListFragmentBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth

private const val WHICH_LIST = "WHICH_LIST"
private const val INITIAL_LIMIT = 10
private const val PERMISSION_ID = 42 //permission ID for location



class MatchListFragment : Fragment() {

    companion object {
        fun newInstance() = MatchListFragment()
    }

    private lateinit var viewModel: MatchListViewModel
    private lateinit var binding: MatchListFragmentBinding
    private val myself = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var application: Application
    private var gpsSwitch: BroadcastReceiver? = null

    //location
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    var latitude: Double? = null
    var longitude: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        application = requireNotNull(this.activity).application
        binding = DataBindingUtil.inflate(inflater,
            R.layout.match_list_fragment,
            container,
            false)

        val which = arguments!!.getInt(WHICH_LIST)
        val viewModelFactory = MatchListViewModelFactory(which)

        viewModel = ViewModelProvider(this, viewModelFactory).get(MatchListViewModel::class.java)

        val adapter = MatchAdapter( MatchListener { matchId, isPublic ->
            viewModel.onMatchClicked(matchId, isPublic)
        })

        binding.matchListViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.matchList.adapter = adapter

        //These functions init the location GPS check
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        getLastLocation()


        // observe list of matches
        viewModel.matches.observe(viewLifecycleOwner, Observer{
            it?.let{
                adapter.submitList(it)
            }
        })

        // pull down to refresh
        binding.swipeHome.setOnRefreshListener {
            if (which == 0) {
                viewModel.getPublicMatches(INITIAL_LIMIT, latitude, longitude,true)
            } else if (which == 1){
                viewModel.getMyMatches(myself, true)
            } else {
                viewModel.getMyArchivedMatches(myself, true)
            }
            binding.swipeHome.isRefreshing = false
        }

        // at the end of the recyclerView in the home, scroll up to load more
        binding.matchList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(dy<=0) return
                //else...
                if(!recyclerView.canScrollVertically(1) && which==0){
                    viewModel.loadOtherPublicMatches(adapter, latitude, longitude)
                }
            }
        })


        // listen to click on Match
        viewModel.navigateToMatchDetail.observe(this.viewLifecycleOwner, Observer { matchId ->
            matchId?.let{
                if(which == 3){
                    var newPath = "/archivedMatches/$myself/"
                    var bundle = bundleOf("matchId" to matchId, "path" to newPath)
                    view?.findNavController()?.navigate(R.id.action_matchListFragment_to_matchFragment, bundle)
                } else {
                    view?.findNavController()?.navigate(
                        HomeFragmentDirections.actionNavigationHomeToMatchFragment(
                            matchId,
                            viewModel.path.value!!
                        )
                    )
                }
                viewModel.onMatchNavigated()
            }
        })

        return binding.root
    }


    //+++++++++START - LOCATION GPS CHECKS+++++++++

    //get Permission for GPS
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    //request Permission for GPS
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this.activity!!,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
        getLastLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Granted. Start getting the location information
                getLastLocation()
            }
        }
    }

    //check whether GPS location is enabled
    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    //get the Location
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val which = arguments!!.getInt(WHICH_LIST)
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(requireNotNull(this.activity)) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        latitude = location.latitude
                        longitude = location.longitude
                        if(which==0){
                            viewModel.getPublicMatches(INITIAL_LIMIT, latitude, longitude, false)
                        } else if(which==1){
                            viewModel.getMyMatches(myself, false)
                        } else {
                            viewModel.getMyArchivedMatches(myself, false)
                        }
                    }
                }
            } else {
                if(which==0){
                    Toast.makeText(requireNotNull(this.activity).applicationContext,
                        "Turn on GPS Location Service to filter matches nearby!", Toast.LENGTH_LONG).show()
                    viewModel.getPublicMatches(INITIAL_LIMIT, latitude, longitude, false)
                } else if(which==1){
                    viewModel.getMyMatches(myself, false)
                } else {
                    viewModel.getMyArchivedMatches(myself, false)
                }
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            val which = arguments!!.getInt(WHICH_LIST)
            latitude = mLastLocation.latitude
            longitude = mLastLocation.longitude
            if(which==0){
                viewModel.getPublicMatches(INITIAL_LIMIT, latitude, longitude, false)
            } else {
                viewModel.getMyMatches(myself, false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val which = arguments!!.getInt(WHICH_LIST)
        //create and register receiver for GPS switch on/off
        if(which==0) {
            gpsSwitch = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if(intent?.action.equals("android.location.PROVIDERS_CHANGED")) {
                        getLastLocation()
                    }
                }
            }
            requireNotNull(this.activity).registerReceiver(
                gpsSwitch,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            )
        }
    }

    override fun onStop(){
        super.onStop()
        val which = arguments!!.getInt(WHICH_LIST)
        if(which==0) {
            requireNotNull(this.activity).unregisterReceiver(gpsSwitch)
        }
    }

}
