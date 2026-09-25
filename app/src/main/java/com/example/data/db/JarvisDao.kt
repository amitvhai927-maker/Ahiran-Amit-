package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    @Query("SELECT * FROM jarvis_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<JarvisHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: JarvisHistoryEntity)

    @Query("DELETE FROM jarvis_history")
    suspend fun clearHistory()

    // Notes & Reminders
    @Query("SELECT * FROM jarvis_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<JarvisNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: JarvisNoteEntity): Long

    @Query("DELETE FROM jarvis_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
}
