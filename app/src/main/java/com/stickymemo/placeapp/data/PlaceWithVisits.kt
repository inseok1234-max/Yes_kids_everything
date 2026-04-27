package com.stickymemo.placeapp.data

import androidx.room.Embedded
import androidx.room.Relation

data class PlaceWithVisits(
    @Embedded val place: PlaceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "placeId"
    )
    val visits: List<VisitRecordEntity>
)
