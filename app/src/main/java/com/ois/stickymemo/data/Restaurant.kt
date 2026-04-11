package com.ois.stickymemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",                  // 식당명
    val location: String = "",              // 주소/위치 텍스트
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Float = 0f,                 // 별점 0.0 ~ 5.0
    val review: String = "",                // 후기
    val tags: String = "",                  // 콤마 구분 태그 ex) "한식,혼밥,가성비"
    val imageUris: String = "",             // 콤마 구분 이미지 URI들
    val recipeUrl: String = "",             // 공유받은 레시피 URL
    val recipeTitle: String = "",           // 레시피 제목
    val visitedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)