package com.stickymemo.placeapp.data

data class PlaceFormSeed(
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
    val revisitIntent: Float = 3f,
    val personalMemo: String = ""
)
