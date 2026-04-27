package com.stickymemo.placeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stickymemo.placeapp.data.PlaceEntity
import com.stickymemo.placeapp.data.PlaceRepository
import kotlinx.coroutines.launch

data class PlaceFormState(
    val id: Long = 0,
    val name: String = "",
    val address: String = "",
    val kakaoPlaceId: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val isYesKids: Boolean = false,
    val strollerFriendly: Boolean = false,
    val hasBabyChair: Boolean = false,
    val hasParking: Boolean = false,
    val hasPlayArea: Boolean = false,
    val priceInfo: String = "",
    val revisitIntent: Int = 3,
    val personalMemo: String = ""
)

class PlaceFormViewModel(private val repository: PlaceRepository) : ViewModel() {
    suspend fun savePlace(state: PlaceFormState) {
        repository.upsertPlace(
            PlaceEntity(
                id = state.id,
                name = state.name,
                address = state.address.ifBlank { null },
                kakaoPlaceId = state.kakaoPlaceId.ifBlank { null },
                latitude = state.latitude.toDoubleOrNull(),
                longitude = state.longitude.toDoubleOrNull(),
                isYesKids = state.isYesKids,
                strollerFriendly = state.strollerFriendly,
                hasBabyChair = state.hasBabyChair,
                hasParking = state.hasParking,
                hasPlayArea = state.hasPlayArea,
                priceInfo = state.priceInfo,
                revisitIntent = state.revisitIntent,
                personalMemo = state.personalMemo
            )
        )
    }

    fun launchSave(state: PlaceFormState, onDone: () -> Unit) {
        viewModelScope.launch {
            savePlace(state)
            onDone()
        }
    }
}

class PlaceFormViewModelFactory(private val repository: PlaceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaceFormViewModel(repository) as T
    }
}
