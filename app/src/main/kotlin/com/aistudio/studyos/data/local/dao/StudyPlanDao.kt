package com.aistudio.studyos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans WHERE isCompleted = 0 AND isDraft = 0 ORDER BY lastUpdated DESC LIMIT 1")
    fun getActivePlan(): Flow<StudyPlanEntity?>

    @Query("SELECT * FROM study_plans WHERE isDraft = 1 OR (isCompleted = 0) ORDER BY lastUpdated DESC")
    fun getSavedPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): StudyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: StudyPlanEntity)

    @Delete
    suspend fun deletePlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)
}
