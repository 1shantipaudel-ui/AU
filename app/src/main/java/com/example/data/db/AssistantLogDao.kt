package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AssistantLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantLogDao {
    @Query("SELECT * FROM assistant_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AssistantLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AssistantLog): Long

    @Query("DELETE FROM assistant_logs")
    suspend fun clearLogs()
}
