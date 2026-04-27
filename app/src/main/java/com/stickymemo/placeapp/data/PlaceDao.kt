package com.stickymemo.placeapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY updatedAt DESC")
    fun observePlaces(): Flow<List<PlaceEntity>>

    @Transaction
    @Query("SELECT * FROM places WHERE id = :placeId")
    fun observePlaceWithVisits(placeId: Long): Flow<PlaceWithVisits?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity): Long

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Insert
    suspend fun insertVisit(visitRecord: VisitRecordEntity)
}
