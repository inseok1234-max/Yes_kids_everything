package com.stickymemo.placeapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickymemo.placeapp.data.PlaceRepository
import com.stickymemo.placeapp.ui.viewmodel.PlaceDetailViewModel
import com.stickymemo.placeapp.ui.viewmodel.PlaceDetailViewModelFactory

@Composable
fun VisitFormScreen(
    placeId: Long,
    repository: PlaceRepository,
    onBack: () -> Unit
) {
    val vm: PlaceDetailViewModel = viewModel(factory = PlaceDetailViewModelFactory(placeId, repository))
    var visitDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(3f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("방문 기록 추가")
        OutlinedTextField(value = visitDate, onValueChange = { visitDate = it }, label = { Text("방문일(YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("방문 메모") }, modifier = Modifier.fillMaxWidth())
        Text("만족도: ${rating.toInt()}/5")
        Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..5f, steps = 3)

        Button(
            onClick = { vm.addVisit(visitDate, note, rating.toInt(), onBack) },
            modifier = Modifier.fillMaxWidth(),
            enabled = visitDate.isNotBlank()
        ) { Text("저장") }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("취소") }
    }
}
