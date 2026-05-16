package com.ois.stickymemo.data

class RestaurantRepository(private val dao: RestaurantDao) {
    fun getAllRestaurants() = dao.getAllRestaurants()

    fun getRestaurantsByRating() = dao.getRestaurantsByRating()

    fun getRestaurantsByTag(tag: String) = dao.getRestaurantsByTag(tag)

    fun getAllTags() = dao.getAllTags()

    suspend fun getRestaurantById(id: Int): Restaurant? = dao.getRestaurantById(id)

    suspend fun insertRestaurant(restaurant: Restaurant): Long = dao.insertRestaurant(restaurant)

    suspend fun updateRestaurant(restaurant: Restaurant) {
        dao.updateRestaurant(restaurant)
    }

    suspend fun deleteRestaurant(restaurant: Restaurant) {
        dao.deleteRestaurant(restaurant)
    }
}
