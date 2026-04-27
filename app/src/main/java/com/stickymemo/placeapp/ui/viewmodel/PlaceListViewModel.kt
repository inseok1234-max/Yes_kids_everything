package com.stickymemo.placeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stickymemo.placeapp.data.PlaceEntity
import com.stickymemo.placeapp.data.PlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlaceListViewModel(private val repository: PlaceRepository) : ViewModel() {
    val places: StateFlow<List<PlaceEntity>> = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class PlaceListViewModelFactory(private val repository: PlaceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaceListViewModel(repository) as T
    }
}
