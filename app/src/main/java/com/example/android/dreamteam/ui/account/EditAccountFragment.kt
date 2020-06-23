package com.example.android.dreamteam.ui.account

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.UserDatabase
import com.example.android.dreamteam.databinding.EditAccountFragmentBinding

class EditAccountFragment : Fragment() {

    companion object {
        fun newInstance() =
            EditAccountFragment()
    }

    private lateinit var viewModel: EditAccountViewModel
    private lateinit var binding: EditAccountFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.edit_account_fragment,
            container,
            false)

        val uid = arguments!!.getString("uid")
        val application = requireNotNull(this.activity).application
        val database = UserDatabase.getInstance(application).userDatabaseDao
        val viewModelFactory =
            EditAccountViewModelFactory(
                uid!!,
                database,
                application
            )

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EditAccountViewModel::class.java)
        binding.editAccountViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // when user is done with editing
        viewModel.navigateBackToAccount.observe(this, Observer { uid ->
            uid?.let{
                val bundle = bundleOf("uid" to uid)
                findNavController().navigate(R.id.action_editAccountFragment_to_accountFragment, bundle)
                viewModel.onNavigated()
            }
        } )

        return binding.root
    }

}
