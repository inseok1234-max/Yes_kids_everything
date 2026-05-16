package com.ois.stickymemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ois.stickymemo.data.MemoDatabase
import com.ois.stickymemo.data.Restaurant
import com.ois.stickymemo.data.RestaurantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RestaurantSortOrder {
    LATEST,
    RATING
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestaurantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RestaurantRepository(
        MemoDatabase.getDatabase(application).restaurantDao()
    )

    private val _sortOrder = MutableStateFlow(RestaurantSortOrder.LATEST)
    val sortOrder: StateFlow<RestaurantSortOrder> = _sortOrder

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    val restaurants: StateFlow<List<Restaurant>> = combine(
        _sortOrder,
        _selectedTag
    ) { sort, tag -> sort to tag }
        .flatMapLatest { (sort, tag) ->
            when {
                tag != null -> repository.getRestaurantsByTag(tag)
                sort == RestaurantSortOrder.RATING -> repository.getRestaurantsByRating()
                else -> repository.getAllRestaurants()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTags: StateFlow<List<String>> = repository.getAllTags()
        .map { tagStrings ->
            tagStrings
                .flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSortOrder(order: RestaurantSortOrder) {
        _sortOrder.value = order
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    fun insertRestaurant(restaurant: Restaurant) {
        viewModelScope.launch { repository.insertRestaurant(restaurant) }
    }

    fun updateRestaurant(restaurant: Restaurant) {
        viewModelScope.launch { repository.updateRestaurant(restaurant) }
    }

    fun deleteRestaurant(restaurant: Restaurant) {
        viewModelScope.launch { repository.deleteRestaurant(restaurant) }
    }
}

class RestaurantViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestaurantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RestaurantViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
