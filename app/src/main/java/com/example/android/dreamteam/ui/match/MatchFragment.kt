package com.example.android.dreamteam.ui.match

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.UserDatabase
import com.example.android.dreamteam.databinding.MatchFragmentBinding
import com.example.android.dreamteam.utils.MatchFragmentStatus
import com.example.android.dreamteam.utils.MatchUserStatus
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.auth.FirebaseAuth


class MatchFragment : Fragment() {

    private lateinit var viewModel: MatchViewModel
    private lateinit var matchId : String
    private lateinit var path: String

    //private var matchAddress: String? = null
    //private var matchCoordinates: List<String> = listOf()
    //private var latitude: Double = 0.0
    //private var longitude: Double = 0.0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: MatchFragmentBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.match_fragment, container, false
        )

        arguments?.let {
            val safeArgs = MatchFragmentArgs.fromBundle(it)
            matchId = safeArgs.matchId!!
            path = safeArgs.path!!
        }

        setHasOptionsMenu(true)

        //mapFragment to viewModelFactory
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.retainInstance = true
        val application = requireNotNull(this.activity).application
        val database = UserDatabase.getInstance(application).userDatabaseDao
        val viewModelFactory = MatchViewModelFactory(matchId, path, database, application, mapFragment)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MatchViewModel::class.java)
        binding.matchViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        //listen on navigationBackToHome
        viewModel.navigateBackToHome.observe(viewLifecycleOwner, Observer{
            it?.let {
                findNavController().navigate(R.id.action_matchFragment_to_navigation_home)
                viewModel.onNavigatedBackToHome()
            }
        })

        //Allow map panning without scroll view interfering with touch event
        val scroll = binding.scroll
        val transparent: ImageView = binding.imagetrans

        transparent.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disallow ScrollView to intercept touch events.
                    scroll.requestDisallowInterceptTouchEvent(true)
                    // Disable touch on transparent view
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events.
                    scroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    scroll.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }

        //listen on navigationToPlayers
        viewModel.navigateToPlayers.observe(viewLifecycleOwner, Observer{
            it?.let{
                var bundle = bundleOf("which" to it, "matchId" to viewModel.matchId,
                    "path" to viewModel.path)
                findNavController().navigate(R.id.action_matchFragment_to_matchPlayersFragment, bundle)
                viewModel.onNavigatedPlayers()
            }
        })

        //listen on navigationToFriends
        viewModel.navigateToFriends.observe(viewLifecycleOwner, Observer{
            it?.let{
                var bundle = bundleOf("which" to it, "matchId" to viewModel.matchId,
                    "path" to viewModel.path)
                findNavController().navigate(R.id.action_matchFragment_to_matchPlayersFragment, bundle)
                viewModel.onNavigatedFriends()
            }
        })

        //listen on navigationToRequests
        viewModel.navigateToRequests.observe(viewLifecycleOwner, Observer{
            it?.let{
                var bundle = bundleOf("which" to it, "matchId" to viewModel.matchId,
                    "path" to viewModel.path)
                findNavController().navigate(R.id.action_matchFragment_to_matchPlayersFragment, bundle)
                viewModel.onNavigatedRequests()
            }
        })

        //listen when map is ready
        viewModel.isMapReady.observe(viewLifecycleOwner, Observer{
            it?.let{
                viewModel.updateMapLocation(viewModel.map)
                viewModel.mapSet()
            }
        })

        //Start Intent to GMAPS when clicking on marker in map
        viewModel.navigateToMaps.observe(this.activity!!, Observer {
            startActivity(viewModel.mapIntent.value)
        })

        return binding.root
    }

    private fun setMarkerClickToGmaps(map: GoogleMap, myAddress: String){
        map.setOnMarkerClickListener{
            /* Intent to search for a Location:
                geo:latitude,longitude?q=query
                geo:0,0?q=my+street+address
                geo:0,0?q=latitude,longitude(label)
            */
            val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=" + Uri.encode(myAddress))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
            true
        }
    }

    // create the chat button
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_options_menu, menu)
    }


    // this is to handle correctly the Chat button!
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId != android.R.id.home) {
            var status = viewModel.matchUserStatus.value!!
            var connection = viewModel.fragmentStatus.value!!
            return when (status){
                MatchUserStatus.OWNER, MatchUserStatus.PARTICIPANT -> {
                    if(connection == MatchFragmentStatus.DEFAULT) {
                        view?.findNavController()
                            ?.navigate(
                                MatchFragmentDirections.actionMatchFragmentToChatFragment(
                                    matchId
                                )
                            )
                    } else {
                        Toast.makeText(requireContext(), "Chat access failed: if you are able to see the match, " +
                                "check your network connection.",
                            Toast.LENGTH_LONG).show()
                    }
                    true
                }
                else -> {
                    if(connection == MatchFragmentStatus.DEFAULT) {
                        Toast.makeText(
                            requireContext(), "You have to be a participant to join the chat!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else {
                        Toast.makeText(requireContext(), "Chat access failed: if you are able to see the match, " +
                                "check your network connection.",
                            Toast.LENGTH_LONG).show()
                    }
                    false
                }
            }
        }
        return false
    }

}
