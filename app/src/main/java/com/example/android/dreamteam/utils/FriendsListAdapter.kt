package com.example.android.dreamteam.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.dreamteam.databinding.ListUserLayoutBinding
import com.example.android.dreamteam.model.User
import com.example.android.dreamteam.ui.match.MatchPlayersViewModel

class FriendsListAdapter(val clickListener: UserListener,
                         val viewModel: MatchPlayersViewModel?,
                         val button1Listener: UserListener?,
                         val button2Listener: UserListener?): ListAdapter<User, FriendsListAdapter.ViewHolder>(
    FriendsListDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(
            parent
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item!!, clickListener, viewModel, button1Listener, button2Listener)
    }

    class ViewHolder private constructor(val binding: ListUserLayoutBinding) : RecyclerView.ViewHolder(binding.root){

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListUserLayoutBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(
                    binding
                )
            }
        }

        fun bind(
            item: User,
            clickListener: UserListener,
            viewModel: MatchPlayersViewModel?,
            button1Listener: UserListener?,
            button2Listener: UserListener?
        ) {
            binding.user = item
            binding.clickListener = clickListener
            binding.matchPlayersViewModel = viewModel
            binding.button1Listener = button1Listener
            binding.button2Listener = button2Listener
            binding.executePendingBindings()
        }
    }

    class FriendsListDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    class UserListener(val clickListener: (friendId: String) -> Unit) {
        fun onClick(user: User) = clickListener(user.uid!!)
    }

}