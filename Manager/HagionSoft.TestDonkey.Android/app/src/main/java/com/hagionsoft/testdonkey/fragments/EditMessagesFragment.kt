package com.hagionsoft.testdonkey.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hagionsoft.testdonkey.R
import com.hagionsoft.testdonkey.viewmodels.EditViewModel
import kotlinx.android.synthetic.main.fragment_edit_messages.*

class EditMessagesFragment : Fragment(), MessageAdapter.ItemClickListener {

    private val model: EditViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: MessageAdapter
    private lateinit var viewManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_edit_messages, container, false)

        recyclerView = root.findViewById(R.id.messageRecyclerView)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        fab.setOnClickListener {
            activity?.let {
                val builder = AlertDialog.Builder(it)
                // Get the layout inflater
                val layoutInflater = requireActivity().layoutInflater

                val view = layoutInflater.inflate(R.layout.dialog_message, null)
                val editText = view.findViewById<EditText>(R.id.message)
                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.ok_button) { dialog, id ->

                        val message = editText.text.toString()
                        if (message.isNotEmpty()) {
                            model.topic?.messages?.add(message)
                            viewAdapter.notifyDataSetChanged()
                        }

                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_button) { dialog, id ->
                        dialog.cancel()
                    }
                builder.create().show()
            }
        }

        context?.let {c ->
            viewManager = LinearLayoutManager(c)
            viewAdapter = MessageAdapter()
            viewAdapter.setOnClickListener(this)

                recyclerView.apply {
                    // use this setting to improve performance if you know that changes
                    // in content do not change the layout size of the RecyclerView
                    //setHasFixedSize(true)

                    // use a linear layout manager
                    layoutManager = viewManager

                    // specify an viewAdapter (see also next example)
                    adapter = viewAdapter
                }
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                viewManager.orientation)
            recyclerView.addItemDecoration(dividerItemDecoration)
        }

        model.topic?.let {
            viewAdapter.setMessages(it.messages)
        }

    }

    override fun onItemClick(view: View?, position: Int) {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val layoutInflater = requireActivity().layoutInflater

            var message = ""
            model.topic?.let {
                message = it.messages[position]
            }
            val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
            val editText = dialogView.findViewById<EditText>(R.id.message)
            editText.setText(message)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton(R.string.ok_button) { dialog, id ->

                    val updatedMessage = editText.text.toString()
                    if (updatedMessage.isNotEmpty()) {
                        model.topic?.let {
                            it.messages[position] = updatedMessage
                            viewAdapter.notifyDataSetChanged()
                        }
                    }

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel_button) { dialog, id ->
                    dialog.cancel()
                }
            builder.create().show()
        }
    }

    override fun onItemLongClick(view: View?, position: Int) {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Delete message?")
                .setPositiveButton(R.string.ok_button) { dialog, id ->

                    model.topic?.messages?.removeAt(position)
                    viewAdapter.notifyDataSetChanged()

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel_button) { dialog, id ->
                    dialog.cancel()
                }
            builder.create().show()
        }
    }
}

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messageDataset = mutableListOf<String>()
    private var clickListener: ItemClickListener? = null

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onItemLongClick(view: View?, position: Int)
    }

    inner class MessageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.name)
        init {
            view.setOnClickListener {
                clickListener?.onItemClick(it, adapterPosition)
            }
            view.setOnLongClickListener {
                clickListener?.onItemLongClick(it, adapterPosition)
                true
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_topic, parent, false)

        return MessageViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.text = messageDataset[position]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = messageDataset.size

    fun setMessages(messages: MutableList<String>) {
        messageDataset = messages
        notifyDataSetChanged()
    }

    fun setOnClickListener(listener: ItemClickListener) {
        this.clickListener = listener
    }
}