package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TunnelRepository(private val dao: TunnelDao) {

    val allProfiles: Flow<List<ProfileEntity>> = dao.getAllProfiles()
    val activeProfile: Flow<ProfileEntity?> = dao.getActiveProfile()
    val sessionHistory: Flow<List<SessionHistoryEntity>> = dao.getAllSessions()
    val logs: Flow<List<LogEntryEntity>> = dao.getLogs()

    suspend fun getProfileById(id: Long): ProfileEntity? = withContext(Dispatchers.IO) {
        dao.getProfileById(id)
    }

    suspend fun insertProfile(profile: ProfileEntity): Long = withContext(Dispatchers.IO) {
        dao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        dao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        dao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteProfileById(id)
    }

    suspend fun setActiveProfile(id: Long) = withContext(Dispatchers.IO) {
        dao.setActiveProfile(id)
    }

    suspend fun duplicateProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        val dup = profile.copy(
            id = 0,
            name = "${profile.name} (Copy)",
            isActive = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertProfile(dup)
    }

    suspend fun recordSession(session: SessionHistoryEntity) = withContext(Dispatchers.IO) {
        dao.insertSession(session)
        dao.pruneOldSessions()
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        dao.clearAllSessions()
    }

    suspend fun addLog(level: String, message: String) = withContext(Dispatchers.IO) {
        dao.insertLog(
            LogEntryEntity(
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message
            )
        )
        dao.pruneLogs()
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        dao.clearLogs()
    }

    suspend fun resetAll() = withContext(Dispatchers.IO) {
        dao.clearLogs()
        dao.clearAllSessions()
        dao.clearAllProfiles()
        DefaultProfiles.createDefaults().forEach {
            dao.insertProfile(it)
        }
    }
}
