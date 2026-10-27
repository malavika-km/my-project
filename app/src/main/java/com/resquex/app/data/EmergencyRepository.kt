package com.resquex.app.data

class EmergencyRepository(
    private val emergencyDao: EmergencyDao
) {

    suspend fun insertEmergency(emergency: EmergencyEntity) {
        emergencyDao.insertEmergency(emergency)
    }

    suspend fun getAllEmergencies(): List<EmergencyEntity> {
        return emergencyDao.getAllEmergencies()
    }

    suspend fun getEmergencyByCategory(
        category: String
    ): EmergencyEntity? {
        return emergencyDao.getEmergencyByCategory(category)
    }

    suspend fun deleteEmergency(emergency: EmergencyEntity) {
        emergencyDao.deleteEmergency(emergency)
    }
}