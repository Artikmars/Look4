package com.artamonov.look4.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artamonov.look4.R
import com.artamonov.look4.data.database.User
import com.artamonov.look4.databinding.ContactItemBinding
import com.google.android.material.snackbar.Snackbar

class ContactListAdapter(
    private val mDataSource: AdapterDataSource<User>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.contact_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feedItem = mDataSource.get(position)
        holder.bindItem(feedItem)
    }

    override fun getItemCount(): Int {
        return mDataSource.count
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val binding = ContactItemBinding.bind(v)
        private var contactItem: User? = null

        fun bindItem(item: User?) {
            this.contactItem = item

            binding.contactName.text = contactItem?.name
            binding.contactPhoneNumber.text = contactItem?.phoneNumber

            binding.contactPhoneNumber.setOnClickListener { v ->
                val myClipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                val myClip = ClipData.newPlainText("label", binding.contactPhoneNumber.text)
                myClipboard.setPrimaryClip(myClip)
                Snackbar.make(v, v.context.resources.getString(R.string.contacts_copied_to_clipboard),
                    Snackbar.LENGTH_SHORT).show()
            }

            binding.contactDeleteIcon.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}
