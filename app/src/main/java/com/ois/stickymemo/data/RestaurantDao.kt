package com.ois.stickymemo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    // 전체 조회 (최신순)
    @Query("SELECT * FROM restaurants ORDER BY createdAt DESC")
    fun getAllRestaurants(): Flow<List<Restaurant>>

    // 별점 높은 순
    @Query("SELECT * FROM restaurants ORDER BY rating DESC")
    fun getRestaurantsByRating(): Flow<List<Restaurant>>

    // 태그 검색 (태그 문자열 포함 여부)
    @Query("SELECT * FROM restaurants WHERE tags LIKE '%' || :tag || '%' ORDER BY rating DESC")
    fun getRestaurantsByTag(tag: String): Flow<List<Restaurant>>

    // 단건 조회
    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getRestaurantById(id: Int): Restaurant?

    // 전체 태그 목록용 (중복 포함 전체)
    @Query("SELECT tags FROM restaurants WHERE tags != ''")
    fun getAllTags(): Flow<List<String>>

    // 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurant(restaurant: Restaurant): Long

    // 수정
    @Update
    suspend fun updateRestaurant(restaurant: Restaurant)

    // 삭제
    @Delete
    suspend fun deleteRestaurant(restaurant: Restaurant)
}