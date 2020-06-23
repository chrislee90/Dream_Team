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
import com.example.android.dreamteam.databinding.AccountFragmentBinding

class AccountFragment : Fragment() {

    companion object {
        fun newInstance() =
            AccountFragment()
    }

    private lateinit var viewModel: AccountViewModel
    private lateinit var binding: AccountFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.account_fragment,
            container,
            false
        )

        val uid: String? = arguments!!.getString("uid")
        val application = requireNotNull(this.activity).application
        val database = UserDatabase.getInstance(application).userDatabaseDao
        val viewModelFactory =
            AccountViewModelFactory(
                uid!!,
                database,
                application
            )


        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AccountViewModel::class.java)
        binding.accountViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // listen if user wants to edit its profile
        viewModel.navigateToEditAccount.observe(viewLifecycleOwner, Observer { uid ->
            uid?.let{
                val bundle = bundleOf("uid" to uid)
                findNavController().navigate(R.id.action_accountFragment_to_editAccountFragment, bundle)
                viewModel.onUserNavigated()
            }
        })
        
        return binding.root
    }


}


