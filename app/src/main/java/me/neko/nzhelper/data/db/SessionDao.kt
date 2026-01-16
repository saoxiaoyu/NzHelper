package me.neko.nzhelper.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Session operations
 */
@Dao
interface SessionDao {
    
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>
    
    @Insert
    suspend fun insert(session: SessionEntity): Long
    
    @Insert
    suspend fun insertAll(sessions: List<SessionEntity>)
    
    @Update
    suspend fun update(session: SessionEntity)
    
    @Delete
    suspend fun delete(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
