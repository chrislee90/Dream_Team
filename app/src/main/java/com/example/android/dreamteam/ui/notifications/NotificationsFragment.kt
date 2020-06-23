package com.example.android.dreamteam.ui.notifications

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.android.dreamteam.R
import com.example.android.dreamteam.database.NotificationDatabase
import com.example.android.dreamteam.databinding.FragmentNotificationsBinding

private const val USERS = "/users/"
private const val PRIVATEMATCH = "privateMatches"
private const val PUBLICMATCH = "publicMatches"

class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_notifications,
            container,
            false
            )

        val application = requireNotNull(this.activity).application
        val dataSource = NotificationDatabase.getInstance(application).notificationDatabaseDao
        val viewModelFactory = NotificationsViewModelFactory(dataSource, application)
        notificationsViewModel =
            ViewModelProviders.of(this, viewModelFactory).get(NotificationsViewModel::class.java)

        binding.notificationsViewModel = notificationsViewModel
        binding.lifecycleOwner = this

        val adapter = NotificationAdapter(NotificationAdapter.NotificationListener { target, id, path ->
            notificationsViewModel.onNotificationClicked(target, id, path)
        })

        binding.notificationsList.adapter = adapter

        notificationsViewModel.notifications!!.observe(viewLifecycleOwner, Observer{
            it?.let{
                adapter.submitList(it)
            }
        })

        // pull down to refresh
        binding.swipeNotifications.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                notificationsViewModel.updateNotifications()
                binding.swipeNotifications.isRefreshing = false
            }
        })

        // listener to click on Notification -> User
        notificationsViewModel.navigateToUser.observe(viewLifecycleOwner, Observer { notification ->
            notification?.let{
                var bundle = bundleOf("uid" to notification)
                findNavController().navigate(
                    R.id.action_navigation_notifications_to_accountFragment,
                    bundle
                )
                notificationsViewModel.onNavigatedUser()
            }
        })

        // listener to click on Notification -> Match
        notificationsViewModel.navigateToMatch.observe(viewLifecycleOwner, Observer { notification ->
            notification?.let{
                var bundle = bundleOf("path" to notificationsViewModel.pathToMatch,
                    "matchId" to notification)
                findNavController().navigate(
                    R.id.action_navigation_notifications_to_matchFragment,
                    bundle
                )
                notificationsViewModel.onNavigatedMatch()
            }
        })

        return binding.root
    }

}
