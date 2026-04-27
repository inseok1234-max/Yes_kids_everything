package com.stickymemo.placeapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stickymemo.placeapp.data.PlaceFormSeed
import com.stickymemo.placeapp.data.PlaceRepository
import com.stickymemo.placeapp.ui.viewmodel.PlaceFormState
import com.stickymemo.placeapp.ui.viewmodel.PlaceFormViewModel
import kotlinx.coroutines.flow.first

@Composable
fun PlaceFormScreen(
    placeId: Long,
    repository: PlaceRepository,
    viewModel: PlaceFormViewModel,
    onDone: () -> Unit
) {
    val seed by produceState(initialValue = PlaceFormSeed(), placeId) {
        value = if (placeId == 0L) {
            PlaceFormSeed()
        } else {
            val p = repository.observePlace(placeId).first()?.place
            if (p == null) PlaceFormSeed() else PlaceFormSeed(
                id = p.id,
                name = p.name,
                address = p.address.orEmpty(),
                kakaoPlaceId = p.kakaoPlaceId.orEmpty(),
                latitude = p.latitude?.toString().orEmpty(),
                longitude = p.longitude?.toString().orEmpty(),
                isYesKids = p.isYesKids,
                strollerFriendly = p.strollerFriendly,
                hasBabyChair = p.hasBabyChair,
                hasParking = p.hasParking,
                hasPlayArea = p.hasPlayArea,
                priceInfo = p.priceInfo,
                revisitIntent = p.revisitIntent.toFloat(),
                personalMemo = p.personalMemo
            )
        }
    }

    var name by remember(seed) { mutableStateOf(seed.name) }
    var address by remember(seed) { mutableStateOf(seed.address) }
    var kakaoPlaceId by remember(seed) { mutableStateOf(seed.kakaoPlaceId) }
    var latitude by remember(seed) { mutableStateOf(seed.latitude) }
    var longitude by remember(seed) { mutableStateOf(seed.longitude) }
    var isYesKids by remember(seed) { mutableStateOf(seed.isYesKids) }
    var strollerFriendly by remember(seed) { mutableStateOf(seed.strollerFriendly) }
    var hasBabyChair by remember(seed) { mutableStateOf(seed.hasBabyChair) }
    var hasParking by remember(seed) { mutableStateOf(seed.hasParking) }
    var hasPlayArea by remember(seed) { mutableStateOf(seed.hasPlayArea) }
    var priceInfo by remember(seed) { mutableStateOf(seed.priceInfo) }
    var revisitIntent by remember(seed) { mutableStateOf(seed.revisitIntent) }
    var personalMemo by remember(seed) { mutableStateOf(seed.personalMemo) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (placeId == 0L) "장소 추가" else "장소 수정")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("장소명") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("주소") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kakaoPlaceId, onValueChange = { kakaoPlaceId = it }, label = { Text("카카오 placeId") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("위도") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("경도") }, modifier = Modifier.fillMaxWidth())

        PlaceBooleanField("예스키즈", isYesKids) { isYesKids = it }
        PlaceBooleanField("유모차 가능", strollerFriendly) { strollerFriendly = it }
        PlaceBooleanField("아기의자 있음", hasBabyChair) { hasBabyChair = it }
        PlaceBooleanField("주차 가능", hasParking) { hasParking = it }
        PlaceBooleanField("놀이공간 있음", hasPlayArea) { hasPlayArea = it }

        OutlinedTextField(value = priceInfo, onValueChange = { priceInfo = it }, label = { Text("가격 정보") }, modifier = Modifier.fillMaxWidth())
        Text("재방문 의사: ${revisitIntent.toInt()}/5")
        Slider(value = revisitIntent, onValueChange = { revisitIntent = it }, valueRange = 1f..5f, steps = 3)
        OutlinedTextField(value = personalMemo, onValueChange = { personalMemo = it }, label = { Text("개인 메모") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                viewModel.launchSave(
                    PlaceFormState(
                        id = seed.id,
                        name = name,
                        address = address,
                        kakaoPlaceId = kakaoPlaceId,
                        latitude = latitude,
                        longitude = longitude,
                        isYesKids = isYesKids,
                        strollerFriendly = strollerFriendly,
                        hasBabyChair = hasBabyChair,
                        hasParking = hasParking,
                        hasPlayArea = hasPlayArea,
                        priceInfo = priceInfo,
                        revisitIntent = revisitIntent.toInt(),
                        personalMemo = personalMemo
                    ),
                    onDone
                )
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("저장") }
    }
}

@Composable
private fun PlaceBooleanField(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
