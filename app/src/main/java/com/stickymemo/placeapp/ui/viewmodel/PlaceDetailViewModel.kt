package com.stickymemo.placeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stickymemo.placeapp.data.PlaceRepository
import com.stickymemo.placeapp.data.PlaceWithVisits
import com.stickymemo.placeapp.data.VisitRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaceDetailViewModel(
    private val placeId: Long,
    private val repository: PlaceRepository
) : ViewModel() {
    val place: StateFlow<PlaceWithVisits?> = flowOf(placeId)
        .flatMapLatest { repository.observePlace(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addVisit(date: String, note: String, rating: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addVisit(
                VisitRecordEntity(
                    placeId = placeId,
                    visitDate = date,
                    note = note,
                    rating = rating
                )
            )
            onDone()
        }
    }
}

class PlaceDetailViewModelFactory(
    private val placeId: Long,
    private val repository: PlaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaceDetailViewModel(placeId, repository) as T
    }
}
