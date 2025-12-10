package com.therangerstar.animation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.therangerstar.animation.data.AttractorDao
import com.therangerstar.animation.data.AttractorSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class AttractorViewModel(private val dao: AttractorDao) : ViewModel() {

    // Expose settings as a Map for easy lookup by attractor name
    val settings: StateFlow<Map<String, AttractorSetting>> = dao.getAllSettings()
        .map { list -> list.associateBy { it.attractorName } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    init {
        initializeColorsIfNeeded()
    }

    private fun initializeColorsIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check each attractor type and assign a random color if not present
            AttractorType.values().forEach { type ->
                if (dao.getSettingSync(type.name) == null) {
                    val randomHue = Random.nextFloat() * 360f
                    // Default saturation to 0.8f for nice colors
                    dao.insert(AttractorSetting(
                        attractorName = type.name, 
                        hue = randomHue, 
                        saturation = 0.8f,
                        speed = 1.0f,
                        particleCount = 8000
                    ))
                }
            }
        }
    }

    fun saveSettings(attractorName: String, hue: Float, saturation: Float, speed: Float, particleCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(AttractorSetting(attractorName, hue, saturation, speed, particleCount))
        }
    }
    
    // Deprecated: kept for backward compatibility if needed, but should forward to saveSettings
    fun saveColor(attractorName: String, hue: Float, saturation: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = dao.getSettingSync(attractorName)
            val speed = current?.speed ?: 1.0f
            val count = current?.particleCount ?: 8000
            dao.insert(AttractorSetting(attractorName, hue, saturation, speed, count))
        }
    }
}

class AttractorViewModelFactory(private val dao: AttractorDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttractorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttractorViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
