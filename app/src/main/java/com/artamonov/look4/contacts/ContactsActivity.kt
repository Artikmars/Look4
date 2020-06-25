package com.artamonov.look4.contacts

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.artamonov.look4.R
import com.artamonov.look4.adapter.AdapterDataSource
import com.artamonov.look4.adapter.ContactListAdapter
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactsState
import com.artamonov.look4.utils.LiveDataContactListState.contactListState
import kotlinx.android.synthetic.main.activity_contacts.*

class ContactsActivity : BaseActivity() {

    private var adapter: ContactListAdapter? = null
    private lateinit var contactsViewModel: ContactsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        contactsViewModel = ViewModelProvider(this).get(ContactsViewModel::class.java)
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
                    return contactsViewModel.getContactList()?.size ?: 0
                }

                override fun get(position: Int): User? {
                    return contactsViewModel.getContactList()?.get(position)
                }
                }, object : ContactListAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (PreferenceHelper.deleteContactItemFromDB(position)) {
                        contactsViewModel.updateList()
                    }
                }
            }
            )

        contacts_list.adapter = adapter
    }

    private fun setNoContactsViewVisibility(state: Boolean) {
        if (state) {
            contacts_placeholder.visibility = VISIBLE
        } else {
            contacts_placeholder.visibility = GONE
        }
    }
}
