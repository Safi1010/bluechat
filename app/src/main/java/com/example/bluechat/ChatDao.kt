package com.example.bluechat

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): LiveData<List<ChatMessage>>

    @Insert
    suspend fun insert(message: ChatMessage)
    
    @Delete
    suspend fun delete(message: ChatMessage)
}
