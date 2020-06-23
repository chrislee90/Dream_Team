package com.example.android.dreamteam.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.dreamteam.databinding.NotificationItemViewBinding
import com.example.android.dreamteam.model.Notification

class NotificationAdapter(val clickListener: NotificationListener): ListAdapter<Notification, NotificationAdapter.ViewHolder>(
    NotificationDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(
            parent
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item!!, clickListener)
    }

    class ViewHolder private constructor(val binding: NotificationItemViewBinding) : RecyclerView.ViewHolder(binding.root){

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = NotificationItemViewBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(
                    binding
                )
            }
        }

        fun bind(
            item: Notification,
            clickListener: NotificationListener
        ) {
            binding.notification = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }

    class NotificationListener(val clickListener: (target: String, id: Int, path: String) -> Unit) {
        fun onClick(notification: Notification) = clickListener(notification.target!!, notification.id!!, notification.path!!)
    }

}