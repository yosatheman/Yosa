package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelDao {

    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfileById(id: Long)

    @Transaction
    suspend fun setActiveProfile(id: Long) {
        clearActiveProfiles()
        setActiveProfileById(id)
    }

    @Query("SELECT * FROM session_history ORDER BY startTime DESC LIMIT 50")
    fun getAllSessions(): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionHistoryEntity)

    @Query("DELETE FROM session_history WHERE id NOT IN (SELECT id FROM session_history ORDER BY startTime DESC LIMIT 50)")
    suspend fun pruneOldSessions()

    @Query("DELETE FROM session_history")
    suspend fun clearAllSessions()

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 300")
    fun getLogs(): Flow<List<LogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntryEntity)

    @Query("DELETE FROM logs")
    suspend fun clearLogs()

    @Query("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneLogs()

    @Query("DELETE FROM profiles")
    suspend fun clearAllProfiles()
}
