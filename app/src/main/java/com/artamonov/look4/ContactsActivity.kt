package com.artamonov.look4

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.artamonov.look4.LookActivity.Companion.CONTACT_LIST
import com.artamonov.look4.adapter.ContactListAdapter
import com.artamonov.look4.data.database.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_contacts.*
import java.lang.reflect.Type

class ContactsActivity : AppCompatActivity() {

    private var adapter: ContactListAdapter? = null
    private var contactList: ArrayList<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        if (getContactList() != null) {
            initAdapter()
        }
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
                    if (deleteItemFromDB(position)) {
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
            )

        contacts_list.adapter = adapter
    }

    private fun getContactList(): ArrayList<User>? {
        val json = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            CONTACT_LIST, "")
        val type: Type = object : TypeToken<List<User?>?>() {}.type
        contactList = Gson().fromJson(json, type)
        return contactList
    }

    private fun deleteItemFromDB(position: Int): Boolean {
        contactList?.removeAt(position)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        val json = Gson().toJson(contactList)
        editor.putString(CONTACT_LIST, json)
        return editor.commit()
    }
}
