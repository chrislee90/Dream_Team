package com.example.android.dreamteam.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.android.dreamteam.ui.friends.FriendsListFragment

private const val WHICH_LIST = "WHICH_LIST"

class FriendsPageAdapter (fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = FriendsListFragment()
        fragment.arguments = Bundle().apply {
            putInt(WHICH_LIST, position)
        }
        return fragment
    }

}