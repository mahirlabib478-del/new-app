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
    // The UI only needs recent history for Home/Progress calculations.
    // Avoid loading an unbounded session-log table into Compose on every startup.
    @Query("SELECT * FROM session_logs WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    fun getLogsSince(sinceMillis: Long): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_logs WHERE strftime('%Y', timestamp / 1000, 'unixepoch', 'localtime') = strftime('%Y', 'now', 'localtime')")
    fun getCurrentYearMinutes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE strftime('%Y', timestamp / 1000, 'unixepoch', 'localtime') = strftime('%Y', 'now', 'localtime')")
    fun getCurrentYearSessionCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_logs WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun getMinutesBetween(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun getSessionCountBetween(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT subject) FROM session_logs WHERE trim(subject) <> ''")
    fun getDistinctSubjectCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT subject) FROM session_logs WHERE trim(subject) <> ''")
    suspend fun getDistinctSubjectCountOnce(): Int

    @Query("""
        SELECT COALESCE(MAX(day_minutes), 0) FROM (
            SELECT date(timestamp / 1000, 'unixepoch', 'localtime') AS study_day,
                   SUM(durationMinutes) AS day_minutes
            FROM session_logs
            GROUP BY study_day
        )
    """)
    fun getPeakDailyFocusMinutes(): Flow<Int>

    @Query("""
        SELECT COALESCE(MAX(day_minutes), 0) FROM (
            SELECT date(timestamp / 1000, 'unixepoch', 'localtime') AS study_day,
                   SUM(durationMinutes) AS day_minutes
            FROM session_logs
            GROUP BY study_day
        )
    """)
    suspend fun getPeakDailyFocusMinutesOnce(): Int


    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_logs WHERE timestamp >= :startOfDayMillis AND timestamp < :startOfNextDayMillis")
    fun getTodayMinutes(startOfDayMillis: Long, startOfNextDayMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_logs WHERE timestamp >= :startOfDayMillis AND timestamp < :startOfNextDayMillis")
    suspend fun getTodayMinutesOnce(startOfDayMillis: Long, startOfNextDayMillis: Long): Int

    @Query("SELECT SUM(durationMinutes) FROM session_logs")
    fun getTotalMinutes(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SessionLogEntity): Long

    @Delete
    suspend fun deleteLog(log: SessionLogEntity)

    @Query("DELETE FROM session_logs")
    suspend fun clearAll()
}
