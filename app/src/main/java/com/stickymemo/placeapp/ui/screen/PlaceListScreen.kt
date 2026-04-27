package com.stickymemo.placeapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickymemo.placeapp.ui.viewmodel.PlaceListViewModel

@Composable
fun PlaceListScreen(
    viewModel: PlaceListViewModel,
    onAdd: () -> Unit,
    onClick: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val places by viewModel.places.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "아이 동반 장소", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onAdd) { Text("추가") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places, key = { it.id }) { place ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onClick(place.id) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(place.address ?: "주소 미입력", style = MaterialTheme.typography.bodySmall)
                        Text("재방문 의사: ${place.revisitIntent}/5")
                        Button(onClick = { onEdit(place.id) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("수정")
                        }
                    }
                }
            }
        }
    }
}
