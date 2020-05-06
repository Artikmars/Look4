package com.artamonov.look4.data

import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.database.UserDao

class UserRepository(private val userDao: UserDao) {

    suspend fun getUser() = userDao.getUser()

    suspend fun updateUser(user: User) { userDao.updateUser(user) }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: UserRepository? = null

        fun getInstance(userDao: UserDao) =
            instance ?: synchronized(this) {
                instance ?: UserRepository(userDao).also { instance = it }
            }
    }
}