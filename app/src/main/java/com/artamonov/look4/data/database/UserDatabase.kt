package com.artamonov.look4.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE = "User"

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class RoomNoteDatabase : RoomDatabase() {

    abstract fun roomNoteDao(): UserDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: RoomNoteDatabase? = null

        fun getInstance(context: Context): RoomNoteDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): RoomNoteDatabase {
            return Room.databaseBuilder(context, RoomNoteDatabase::class.java, DATABASE)
                .build()
        }
    }
}