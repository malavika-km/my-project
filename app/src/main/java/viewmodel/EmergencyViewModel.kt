package com.resquex.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resquex.app.data.EmergencyDatabase
import com.resquex.app.data.EmergencyEntity
import com.resquex.app.data.EmergencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmergencyViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = EmergencyRepository(
        EmergencyDatabase.getDatabase(application).emergencyDao()
    )

    private val _emergencies =
        MutableStateFlow<List<EmergencyEntity>>(emptyList())

    val emergencies: StateFlow<List<EmergencyEntity>> = _emergencies

    private val _isLoading = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> = _errorMessage


    fun loadEmergencies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val existingEmergencies = repository.getAllEmergencies()

                if (existingEmergencies.isEmpty()) {
                    val sampleEmergencies = listOf(
                        EmergencyEntity(
                            category = "Severe Bleeding",
                            instructions = "Apply firm, direct pressure to the wound with a clean cloth. Seek emergency medical help."
                        ),
                        EmergencyEntity(
                            category = "Burns",
                            instructions = "Cool the burn under cool running water for 20 minutes. Do not apply ice. Seek medical help for serious burns."
                        ),
                        EmergencyEntity(
                            category = "Choking",
                            instructions = "If the person cannot breathe, speak, or cough, call emergency services and follow the appropriate first-aid procedure."
                        ),
                        EmergencyEntity(
                            category = "Cardiac Arrest",
                            instructions = "Call emergency services immediately. If the person is unresponsive and not breathing normally, begin CPR and use an AED if available."
                        ),
                        EmergencyEntity(
                            category = "Electric Shock",
                            instructions = "Do not touch the person while they may still be in contact with electricity. Disconnect the power if safe and call emergency services."
                        ),
                        EmergencyEntity(
                            category = "Unconscious Person",
                            instructions = "Check responsiveness and normal breathing. Call emergency services. If the person is not breathing normally, begin CPR."
                        )
                    )

                    sampleEmergencies.forEach { emergency ->
                        repository.insertEmergency(emergency)
                    }
                }

                _emergencies.value = repository.getAllEmergencies()

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Unable to load emergency guidance. Please try again."

            } finally {
                _isLoading.value = false
            }
        }
    }



    fun addEmergency(
        category: String,
        instructions: String
    ) {
        viewModelScope.launch {
            repository.insertEmergency(
                EmergencyEntity(
                    category = category,
                    instructions = instructions
                )
            )

            loadEmergencies()
        }
    }
}