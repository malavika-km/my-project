    package com.resquex.app.data

    import androidx.room.Dao
    import androidx.room.Delete
    import androidx.room.Insert
    import androidx.room.OnConflictStrategy
    import androidx.room.Query

    @Dao
    interface EmergencyDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertEmergency(emergency: EmergencyEntity)

        @Query("SELECT * FROM emergency_guidance ORDER BY category ASC")
        suspend fun getAllEmergencies(): List<EmergencyEntity>

        @Query("SELECT * FROM emergency_guidance WHERE category = :category LIMIT 1")
        suspend fun getEmergencyByCategory(category: String): EmergencyEntity?

        @Delete
        suspend fun deleteEmergency(emergency: EmergencyEntity)
    }