package com.example.android.dreamteam.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ListView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.dreamteam.databinding.ChatMyMessageBinding
import com.example.android.dreamteam.databinding.ChatTheirMessageBinding

class MessageAdapter() : ListAdapter<Message, RecyclerView.ViewHolder>(MatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        if (viewType==0) return MyMessageViewHolder.from(parent)
        else return TheirMessageViewHolder.from(parent)
    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder.itemViewType == 0) {
            (holder as MyMessageViewHolder).bind(getItem(position))
        }
        else{
            (holder as TheirMessageViewHolder).bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (getItem(position).mine) return 0  //myMessage
        else return 1  // their message

    }

    class MyMessageViewHolder private constructor(val binding: ChatMyMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(item: Message) {
            binding.message = item
        }

        companion object {
            fun from(parent: ViewGroup): MyMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatMyMessageBinding.inflate(layoutInflater, parent, false)
                return MyMessageViewHolder(binding)
            }
        }
    }

    class TheirMessageViewHolder private constructor(val binding: ChatTheirMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(item: Message) {
            binding.message = item
        }

        companion object {
            fun from(parent: ViewGroup): TheirMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatTheirMessageBinding.inflate(layoutInflater, parent, false)
                return TheirMessageViewHolder(binding)
            }
        }
    }
}

class MatchDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}
