package com.example.android.dreamteam.ui.friends

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.SearchView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.android.dreamteam.R
import com.example.android.dreamteam.databinding.FriendsFragmentBinding
import com.example.android.dreamteam.utils.FriendsListAdapter
import com.example.android.dreamteam.utils.FriendsPageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsFragment : Fragment() {

    private lateinit var viewModel: FriendsViewModel
    private lateinit var binding: FriendsFragmentBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var friendsPageAdapter: FriendsPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // notify on creation that options menu is available here
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(FriendsViewModel::class.java)
        val adapter =
            FriendsListAdapter(
                FriendsListAdapter.UserListener { uid ->
                    viewModel.onUserClicked(uid)
                }, null, null, null)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.friends_fragment,
            container,
            false
        )
        binding.friendsViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.searchFriends.adapter = adapter
        viewModel.search.observe(viewLifecycleOwner, Observer{
            it?.let {
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        friendsPageAdapter = FriendsPageAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = friendsPageAdapter
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if(position==0) tab.text = "Friends"
            else tab.text = "Requests"
        }.attach()
    }


    // create the options menu = create the search bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(com.example.android.dreamteam.R.menu.options_menu, menu)
        val item: MenuItem = menu.findItem(com.example.android.dreamteam.R.id.search)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        // now handle the search, if any is present
        val searchView: SearchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                //this is where the query submitted by the user is processed
                viewModel.getUsersByName(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                //this could be used to submit suggestions to the user
                return true
            }
        })

        searchView.setOnCloseListener(object : SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                viewModel.closeSearch()
                return true
            }
        })
    }

    // this is to handle correctly the back button of the search View!
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            viewModel.closeSearch()
            return true
        }
        return false
    }


}

