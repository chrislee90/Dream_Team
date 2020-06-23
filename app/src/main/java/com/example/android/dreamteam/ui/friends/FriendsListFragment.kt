package com.example.android.dreamteam.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.android.dreamteam.R
import com.example.android.dreamteam.databinding.FragmentListFriendsBinding
import com.example.android.dreamteam.utils.FriendsListAdapter
import com.google.firebase.auth.FirebaseAuth

private const val WHICH_LIST = "WHICH_LIST"

class FriendsListFragment: Fragment(){

    private lateinit var viewModel: FriendsListViewModel
    private lateinit var binding: FragmentListFriendsBinding
    private val myself = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_list_friends,
            container,
            false
        )

        val which: Int = arguments!!.getInt(WHICH_LIST)
        val viewModelFactory = FriendsListViewModelFactory(which)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(FriendsListViewModel::class.java)
        val adapter =
            FriendsListAdapter(
                FriendsListAdapter.UserListener { uid ->
                    viewModel.onUserClicked(uid)
                }, null, null, null)

        binding.friendsListViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.listFriends.adapter = adapter

        // now observe to the list of users
        viewModel.friends.observe(viewLifecycleOwner, Observer{
            it?.let{
                adapter.submitList(it)
            }
        })

        // listener to click on User
        viewModel._navigateToUserFragment.observe(viewLifecycleOwner, Observer { user ->
            user?.let{
                val bundle = bundleOf("uid" to user)
                findNavController().navigate(R.id.action_navigation_friends_to_accountFragment, bundle)
                viewModel.onUserNavigated()
            }
        })

        if(which==0){
            viewModel.getUserFriends(myself)
        } else {
            viewModel.getUserFriendRequests(myself)
        }

        return binding.root
    }


}