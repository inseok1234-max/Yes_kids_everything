package com.stickymemo.placeapp.ui.navigation

sealed class Routes(val route: String) {
    data object PlaceList : Routes("place_list")
    data object PlaceForm : Routes("place_form?placeId={placeId}") {
        fun create(placeId: Long? = null): String = "place_form?placeId=${placeId ?: 0}"
    }

    data object PlaceDetail : Routes("place_detail/{placeId}") {
        fun create(placeId: Long): String = "place_detail/$placeId"
    }

    data object VisitForm : Routes("visit_form/{placeId}") {
        fun create(placeId: Long): String = "visit_form/$placeId"
    }
}
