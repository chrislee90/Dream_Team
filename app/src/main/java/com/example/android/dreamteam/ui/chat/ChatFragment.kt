package com.example.android.dreamteam.ui.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.example.android.dreamteam.R
import com.example.android.dreamteam.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private lateinit var viewModel: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding : FragmentChatBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_chat, container, false
        )

        val matchId = ChatFragmentArgs.fromBundle(arguments!!).matchId

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        viewModel.startChat(requireContext(), matchId)

        binding.lifecycleOwner = this
        binding.chatViewModel = viewModel



        val adapter = MessageAdapter()
        binding.messagesView.adapter = adapter

        viewModel.messageList.observe(viewLifecycleOwner, Observer {
            it?.let{
                adapter.submitList(it)
            }
        })

        return binding.root
    }

}
