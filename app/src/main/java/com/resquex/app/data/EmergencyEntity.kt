package com.resquex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_guidance")
data class EmergencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val instructions: String
)