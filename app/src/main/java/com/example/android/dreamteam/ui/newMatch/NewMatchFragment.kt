package com.example.android.dreamteam.ui.newMatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.MatchDatabase
import com.example.android.dreamteam.database.UserDatabase
import com.example.android.dreamteam.databinding.FragmentNewMatchBinding
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.places.model.PlaceFields
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.util.*


class NewMatchFragment : Fragment() {

    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val binding : FragmentNewMatchBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_new_match, container, false
        )

        val application = requireNotNull(this.activity).application
        val dataSource = MatchDatabase.getInstance(application).matchDatabaseDao
        val dataUser = UserDatabase.getInstance(application).userDatabaseDao
        val viewModelFactory = NewMatchViewModelFactory(dataSource, dataUser, application)
        val newMatchViewModel =
            ViewModelProviders.of(
                this, viewModelFactory).get(NewMatchViewModel::class.java)

        binding.lifecycleOwner = this
        binding.newMatchViewModel = newMatchViewModel


        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            newMatchViewModel.updateDate(LocalDate.of(year, monthOfYear+1, dayOfMonth))
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            newMatchViewModel.updateTime(LocalTime.of(hourOfDay, minute))
        }

        binding.datePicker.setOnClickListener {
            val d = newMatchViewModel.getMatchDate()
            DatePickerDialog(this.requireContext(), dateSetListener,
                d.year,
                d.monthValue-1,
                d.dayOfMonth).show()
        }

        binding.timePicker.setOnClickListener {
            val d = newMatchViewModel.getMatchDate()
            TimePickerDialog(this.requireContext(), timeSetListener,
                d.hour,
                d.minute,
                true).show()
        }

        binding.createMatchBtn.setOnClickListener { view ->
            newMatchViewModel.onSaveMatch()
            view.findNavController().navigate(R.id.action_newMatchFragment_to_navigation_home2)
        }
        //Google Places-----------------------------------------------------------------------------
        // Create a new Places client instance.
        //qualified "this"
        placesClient = Places.createClient(activity!!)

        // Initialize the AutocompleteSupportFragment.
        val autocompleteSupportFragment = childFragmentManager
            .findFragmentById(R.id.autocomplete_support_fragment) as AutocompleteSupportFragment?
        //throw null exception on autocompleteSupportFragment
        autocompleteSupportFragment!!.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteSupportFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                //Get info about the selected place.
                Log.i(TAG, "Place: " + place.name + ", " + place.latLng.toString()
                    .removePrefix("lat/lng: (").removeSuffix(")"))

                //assign to google places selected place and latLng to textviews removing additional chars from latlng
                binding.matchPlaceAutocompletedTextView.text = place.name
                binding.latLngTextView.text = place.latLng.toString()
                    .removePrefix("lat/lng: (").removeSuffix(")")
            }

            override fun onError(status: Status) {
                //Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }
        })

        return binding.root
    }


}