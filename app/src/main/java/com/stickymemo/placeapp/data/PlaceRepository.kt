package com.stickymemo.placeapp.data

import kotlinx.coroutines.flow.Flow

class PlaceRepository(private val placeDao: PlaceDao) {
    fun observePlaces(): Flow<List<PlaceEntity>> = placeDao.observePlaces()

    fun observePlace(placeId: Long): Flow<PlaceWithVisits?> = placeDao.observePlaceWithVisits(placeId)

    suspend fun upsertPlace(place: PlaceEntity): Long {
        return if (place.id == 0L) {
            placeDao.insertPlace(place)
        } else {
            placeDao.updatePlace(place.copy(updatedAt = System.currentTimeMillis()))
            place.id
        }
    }

    suspend fun addVisit(visit: VisitRecordEntity) {
        placeDao.insertVisit(visit)
    }
}
