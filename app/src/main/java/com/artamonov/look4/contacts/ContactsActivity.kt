package com.artamonov.look4.contacts

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.artamonov.look4.R
import com.artamonov.look4.adapter.AdapterDataSource
import com.artamonov.look4.adapter.ContactListAdapter
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.utils.ContactsState
import com.artamonov.look4.utils.LiveDataContactListState.contactListState
import kotlinx.android.synthetic.main.activity_contacts.*

class ContactsActivity : BaseActivity(R.layout.activity_contacts) {

    private var adapter: ContactListAdapter? = null

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contacts_back.setOnClickListener { onBackPressed() }

        contactListState.observe(this, Observer { state ->
            when (state) {
                ContactsState.LoadedState -> {
                    initAdapter()
                    setNoContactsViewVisibility(false)
                }
                ContactsState.UpdatedListState -> {
                    setNoContactsViewVisibility(false)
                    if (adapter == null) {
                        initAdapter()
                        return@Observer
                    }
                    adapter?.notifyDataSetChanged()
                }

                ContactsState.NoContactsState -> {
                    setNoContactsViewVisibility(true)
                }
            }
        })

        contactsViewModel.initList()
    }

    private fun initAdapter() {
        val linearLayoutManager = LinearLayoutManager(this)
        contacts_list.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(this, linearLayoutManager.orientation)
        contacts_list.addItemDecoration(dividerItemDecoration)

        adapter = ContactListAdapter(
            object : AdapterDataSource<User> {
                override fun getCount(): Int {
                    return contactsViewModel.getContactList().size
                }

                override fun get(position: Int): User? {
                    return contactsViewModel.getContactList()[position]
                }
            }, object : ContactListAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (prefs.deleteContactItemFromDB(position)) {
                        contactsViewModel.updateList()
                    }
                }
            }
        )

        contacts_list.adapter = adapter
    }

    private fun setNoContactsViewVisibility(state: Boolean) {
        contacts_placeholder.isVisible = state
    }
}
