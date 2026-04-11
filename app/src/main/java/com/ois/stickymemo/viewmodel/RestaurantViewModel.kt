package com.ois.stickymemo.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ois.stickymemo.data.MemoDatabase
import com.ois.stickymemo.data.Restaurant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class RestaurantSortOrder {
    LATEST, RATING
}

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MemoDatabase.getDatabase(application).restaurantDao()

    // 정렬 상태
    private val _sortOrder = MutableStateFlow(RestaurantSortOrder.LATEST)
    val sortOrder: StateFlow<RestaurantSortOrder> = _sortOrder

    // 선택된 태그 필터
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    // 정렬 + 필터 조합된 최종 목록
    val restaurants: StateFlow<List<Restaurant>> = combine(
        _sortOrder,
        _selectedTag
    ) { sort, tag -> Pair(sort, tag) }
        .flatMapLatest { (sort, tag) ->
            when {
                tag != null -> dao.getRestaurantsByTag(tag)
                sort == RestaurantSortOrder.RATING -> dao.getRestaurantsByRating()
                else -> dao.getAllRestaurants()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 전체 태그 목록 (중복 제거)
    val allTags: StateFlow<List<String>> = dao.getAllTags()
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
        viewModelScope.launch { dao.insertRestaurant(restaurant) }
    }

    fun updateRestaurant(restaurant: Restaurant) {
        viewModelScope.launch { dao.updateRestaurant(restaurant) }
    }

    fun deleteRestaurant(restaurant: Restaurant) {
        viewModelScope.launch { dao.deleteRestaurant(restaurant) }
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