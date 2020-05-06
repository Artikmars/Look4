package com.artamonov.look4.data.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UserDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insert(user: User?)
//
//    @Update(onConflict = OnConflictStrategy.REPLACE)
//    fun query(user: User?)
//
//    @Query("DELETE FROM User")
//    fun deleteAll()
//
//    @get:Query("SELECT * FROM User EXCEPT SELECT * FROM User WHERE gender != null")
//    val allContacts: LiveData<List<User?>?>?
//
//    @get:Query("SELECT * FROM User WHERE gender != null")
//    val userData: LiveData<User?>?

    @Query("SELECT * FROM User WHERE gender != null")
    suspend fun getUser(): User

    suspend fun updateUser (user: User)
}