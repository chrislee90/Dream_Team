package com.example.android.dreamteam.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.dreamteam.databinding.MatchItemViewBinding
import com.example.android.dreamteam.model.Match

class MatchAdapter(val clickListener: MatchListener) : ListAdapter<Match, MatchAdapter.ViewHolder>(MatchDiffCallback())  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }


    class ViewHolder private constructor(val binding: MatchItemViewBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(item: Match, clickListener: MatchListener) {
            binding.match = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = MatchItemViewBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}


class MatchDiffCallback : DiffUtil.ItemCallback<Match>() {
    override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean {
        return oldItem == newItem
    }
}

class MatchListener(val clickListener: (matchId: String, isPublic: Boolean) -> Unit){
    fun onClick(match: Match) = clickListener(match.id, match.isPublic!!)
}

