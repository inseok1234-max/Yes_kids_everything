package com.stickymemo.placeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String?,
    val kakaoPlaceId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isYesKids: Boolean,
    val strollerFriendly: Boolean,
    val hasBabyChair: Boolean,
    val hasParking: Boolean,
    val hasPlayArea: Boolean,
    val priceInfo: String,
    val revisitIntent: Int,
    val personalMemo: String,
    val updatedAt: Long = System.currentTimeMillis()
)
