package com.artamonov.look4.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.artamonov.look4.R
import com.artamonov.look4.data.database.User
import kotlinx.android.synthetic.main.contact_item.view.*

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

        private var view: View = v
        private var contactItem: User? = null

        fun bindItem(item: User?) {
            this.contactItem = item

            view.contact_name.text = contactItem?.name
            view.contact_phone_number.text = contactItem?.phoneNumber

            view.contact_phone_number.setOnClickListener { v ->
                val myClipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                val myClip = ClipData.newPlainText("label", view.contact_phone_number.text.toString())
                myClipboard.setPrimaryClip(myClip)
                Toast.makeText(v.context, "Copied to clipboard.", Toast.LENGTH_SHORT).show()
            }

            view.contact_delete_icon.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}