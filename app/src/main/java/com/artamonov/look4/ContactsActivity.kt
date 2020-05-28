package com.artamonov.look4

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.artamonov.look4.adapter.ContactListAdapter
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import kotlinx.android.synthetic.main.activity_contacts.*

class ContactsActivity : BaseActivity() {

    private var adapter: ContactListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        contacts_back.setOnClickListener { onBackPressed() }

        if (!getContactList().isNullOrEmpty()) { initAdapter() }
        setNoContactsViewVisibility(getContactList().isNullOrEmpty())
    }

    private fun initAdapter() {
        val linearLayoutManager = LinearLayoutManager(this)
        contacts_list.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(this, linearLayoutManager.orientation)
        contacts_list.addItemDecoration(dividerItemDecoration)

        adapter = ContactListAdapter(
            object : AdapterDataSource<User> {
                override fun getCount(): Int {
                    return getContactList()!!.size
                }

                override fun get(position: Int): User? {
                    return getContactList()!![position]
                }
                }, object : ContactListAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (PreferenceHelper.deleteContactItemFromDB(position)) {
                        adapter?.notifyDataSetChanged()
                        setNoContactsViewVisibility(getContactList().isNullOrEmpty())
                    }
                }
            }
            )

        contacts_list.adapter = adapter
    }

    private fun getContactList(): ArrayList<User>? {
        return PreferenceHelper.getContactList()
    }

    private fun setNoContactsViewVisibility(state: Boolean) {
        if (state) {
            contacts_placeholder.visibility = VISIBLE
        } else {
            contacts_placeholder.visibility = GONE
        }
    }
}
