package com.aistudio.studyos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>>

    @Query("SELECT SUM(durationMinutes) FROM session_logs")
    fun getTotalMinutes(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SessionLogEntity): Long

    @Delete
    suspend fun deleteLog(log: SessionLogEntity)

    @Query("DELETE FROM session_logs")
    suspend fun clearAll()
}
