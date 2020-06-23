package com.example.android.dreamteam.ui.match

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
import com.example.android.dreamteam.databinding.FragmentListFriendsBinding
import com.example.android.dreamteam.databinding.MatchPlayersFragmentBinding
import com.example.android.dreamteam.ui.friends.FriendsListViewModel
import com.example.android.dreamteam.ui.friends.FriendsListViewModelFactory
import com.example.android.dreamteam.utils.FriendsListAdapter
import com.google.firebase.auth.FirebaseAuth

private const val JOIN = "JOIN"
private const val DENY = "DENY"
private const val NAVIGATE = "NAVIGATE"
//private const val INVITE = "INVITE"
//private const val EXPEL = "EXPEL"

class MatchPlayersFragment : Fragment() {

    companion object {
        fun newInstance() = MatchPlayersFragment()
    }

    private lateinit var viewModel: MatchPlayersViewModel
    private lateinit var binding: MatchPlayersFragmentBinding
    private val myself = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.match_players_fragment,
            container,
            false
        )

        val matchId = arguments!!.getString("matchId")!!
        val path = arguments!!.getString("path")!!
        val which = arguments!!.getString("which")!!

        val viewModelFactory = MatchPlayersViewModelFactory(matchId, path, which)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MatchPlayersViewModel::class.java)
        val adapter =
            FriendsListAdapter(
                FriendsListAdapter.UserListener { uid ->
                    viewModel.onUserClicked(uid, NAVIGATE)
                }, viewModel, FriendsListAdapter.UserListener { uid ->
                    viewModel.onUserClicked(uid, JOIN)
                }, FriendsListAdapter.UserListener { uid ->
                    viewModel.onUserClicked(uid, DENY)
                })

        binding.matchPlayersListViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.listPlayers.adapter = adapter


        // now observe to the list of users
        viewModel.players.observe(viewLifecycleOwner, Observer{
            it?.let{
                adapter.submitList(it)
            }
        })

        // listener to click on User
        viewModel.navigateToUserFragment.observe(viewLifecycleOwner, Observer { user ->
            user?.let{
                val bundle = bundleOf("uid" to user)
                findNavController().navigate(R.id.action_matchPlayersFragment_to_accountFragment, bundle)
                viewModel.onUserNavigated()
            }
        })



        return binding.root
    }

}
