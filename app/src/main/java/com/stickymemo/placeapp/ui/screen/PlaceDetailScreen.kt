package com.stickymemo.placeapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickymemo.placeapp.ui.viewmodel.PlaceDetailViewModel

@Composable
fun PlaceDetailScreen(
    viewModel: PlaceDetailViewModel,
    onBack: () -> Unit,
    onAddVisit: () -> Unit
) {
    val state by viewModel.place.collectAsStateWithLifecycle()
    val place = state?.place ?: return

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("장소 상세")
        Text(place.name)
        Text(place.address ?: "주소 없음")
        Text("예스키즈: ${place.isYesKids}")
        Text("유모차: ${place.strollerFriendly}")
        Text("아기의자: ${place.hasBabyChair}")
        Text("주차: ${place.hasParking}")
        Text("놀이공간: ${place.hasPlayArea}")
        Text("가격: ${place.priceInfo}")
        Text("재방문 의사: ${place.revisitIntent}/5")
        Text("메모: ${place.personalMemo}")
        Text("좌표: ${place.latitude ?: "-"}, ${place.longitude ?: "-"}")
        Text("카카오 placeId: ${place.kakaoPlaceId ?: "-"}")

        Button(onClick = onAddVisit, modifier = Modifier.fillMaxWidth()) { Text("방문 기록 추가") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state?.visits.orEmpty(), key = { it.id }) { visit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("방문일: ${visit.visitDate}")
                        Text("만족도: ${visit.rating}/5")
                        Text(visit.note)
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("뒤로") }
    }
}
