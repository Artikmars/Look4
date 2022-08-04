package com.artamonov.look4.contacts

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.artamonov.look4.adapter.AdapterDataSource
import com.artamonov.look4.adapter.ContactListAdapter
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.databinding.ActivityContactsBinding
import com.artamonov.look4.utils.ContactsState
import com.artamonov.look4.utils.LiveDataContactListState.contactListState

class ContactsActivity : BaseActivity() {

    private var adapter: ContactListAdapter? = null
    private lateinit var binding: ActivityContactsBinding

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.contactsBack.setOnClickListener { onBackPressed() }

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
                else -> {
                    // nothing
                }
            }
        })

        contactsViewModel.initList()
    }

    private fun initAdapter() {
        val linearLayoutManager = LinearLayoutManager(this)
        binding.contactsList.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(this, linearLayoutManager.orientation)
        binding.contactsList.addItemDecoration(dividerItemDecoration)

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

        binding.contactsList.adapter = adapter
    }

    private fun setNoContactsViewVisibility(state: Boolean) {
        binding.contactsPlaceholder.isVisible = state
    }
}
