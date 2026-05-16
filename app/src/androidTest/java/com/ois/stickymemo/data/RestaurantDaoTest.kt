package com.ois.stickymemo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RestaurantDaoTest {
    private lateinit var database: MemoDatabase
    private lateinit var dao: RestaurantDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.restaurantDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getRestaurantsByRating_sortsHighestFirst() = runBlocking {
        dao.insertRestaurant(Restaurant(name = "three", rating = 3f))
        dao.insertRestaurant(Restaurant(name = "five", rating = 5f))
        dao.insertRestaurant(Restaurant(name = "four", rating = 4f))

        val restaurants = dao.getRestaurantsByRating().first()

        assertEquals(listOf("five", "four", "three"), restaurants.map { it.name })
    }

    @Test
    fun getRestaurantsByTag_filtersAndSortsByRating() = runBlocking {
        dao.insertRestaurant(Restaurant(name = "sushi", rating = 4f, tags = "japanese, date"))
        dao.insertRestaurant(Restaurant(name = "ramen", rating = 5f, tags = "japanese"))
        dao.insertRestaurant(Restaurant(name = "taco", rating = 5f, tags = "mexican"))

        val restaurants = dao.getRestaurantsByTag("japanese").first()

        assertEquals(listOf("ramen", "sushi"), restaurants.map { it.name })
    }
}
