package com.ois.stickymemo.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import coil.compose.AsyncImage
import com.ois.stickymemo.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun RestaurantEditScreen(
    restaurant: Restaurant? = null,
    existingTags: List<String> = emptyList(),
    onSave: (Restaurant) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = restaurant != null

    var name by remember { mutableStateOf(restaurant?.name ?: "") }
    var location by remember { mutableStateOf(restaurant?.location ?: "") }
    var review by remember { mutableStateOf(restaurant?.review ?: "") }
    var rating by remember { mutableStateOf(restaurant?.rating ?: 0f) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember {
        mutableStateOf(
            if (restaurant?.tags.isNullOrBlank()) emptyList()
            else restaurant!!.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        )
    }
    var imageUris by remember {
        mutableStateOf(
            if (restaurant?.imageUris.isNullOrBlank()) emptyList()
            else restaurant!!.imageUris.split(",").filter { it.isNotBlank() }
        )
    }
    var locationSearchResults by remember { mutableStateOf<List<AddressResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var recipeUrl by remember { mutableStateOf(restaurant?.recipeUrl ?: "") }
    var recipeTitle by remember { mutableStateOf(restaurant?.recipeTitle ?: "") }

    // 이미지 선택
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newUris = uris.map { it.toString() }
        imageUris = (imageUris + newUris).distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "맛집 수정" else "맛집 추가",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.isBlank()) return@TextButton
                        val r = (restaurant ?: Restaurant()).copy(
                            name = name.trim(),
                            location = location.trim(),
                            review = review.trim(),
                            rating = rating,
                            tags = tags.joinToString(","),
                            imageUris = imageUris.joinToString(","),
                            recipeUrl = recipeUrl.trim(),
                            recipeTitle = recipeTitle.trim()
                        )
                        onSave(r)
                    }) {
                        Text("저장", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 식당명 ──
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("식당명 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

// ── 위치 ──
            Column {
                OutlinedTextField(
                    value = location,
                    onValueChange = { newValue ->
                        location = newValue
                        locationSearchResults = emptyList()
                        searchJob?.cancel()
                        if (newValue.length >= 2) {
                            isSearching = true
                            searchJob = scope.launch {
                                delay(500)
                                locationSearchResults = searchAddress(newValue)
                                isSearching = false
                            }
                        } else {
                            isSearching = false
                        }
                    },
                    label = { Text("위치 / 주소") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                // 검색 결과 드롭다운
                if (locationSearchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            locationSearchResults.take(5).forEach { result ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            location = result.address.ifBlank { result.name }
                                            locationSearchResults = emptyList()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = result.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = result.address,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // ── 별점 ──
            Column {
                Text(
                    "별점",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                RatingBar(
                    rating = rating,
                    onRatingChanged = { rating = it }
                )
            }

            // ── 후기 ──
            OutlinedTextField(
                value = review,
                onValueChange = { review = it },
                label = { Text("후기") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // ── 태그 ──
            Column {
                Text(
                    "태그",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("태그 입력") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = {
                        val t = tagInput.trim()
                        if (t.isNotBlank() && !tags.contains(t)) {
                            tags = tags + t
                        }
                        tagInput = ""
                    }) { Text("추가") }
                }
                // 기존 태그 추천
                if (existingTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingTags.filter { !tags.contains(it) }.forEach { t ->
                            SuggestionChip(
                                onClick = { tags = tags + t },
                                label = { Text(t, fontSize = 12.sp) }
                            )
                        }
                    }
                }
                // 선택된 태그
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { t ->
                            InputChip(
                                selected = true,
                                onClick = { tags = tags - t },
                                label = { Text(t, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "삭제",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── 이미지 ──
            Column {
                Text(
                    "사진",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 추가 버튼
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "사진 추가",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 선택된 이미지들
                    imageUris.forEach { uri ->
                        Box(modifier = Modifier.size(90.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUris = imageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "삭제",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 레시피 ──
            Column {
                Text(
                    "레시피",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "다른 앱(만개의레시피, 네이버 등)에서 공유 버튼 → StickyMemo 선택",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (recipeUrl.isNotBlank()) {
                    // 레시피 카드
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    recipeTitle.ifBlank { "레시피 링크" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    recipeUrl,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(recipeUrl))
                                )
                            }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "열기")
                            }
                            IconButton(onClick = {
                                recipeUrl = ""
                                recipeTitle = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "삭제")
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = recipeUrl,
                        onValueChange = { recipeUrl = it },
                        label = { Text("레시피 URL (직접 입력)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── 별점 입력 컴포넌트 ──
@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    maxStars: Int = 5
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..maxStars) {
            val filled = i <= rating
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "$i 점",
                tint = if (filled) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChanged(i.toFloat()) }
            )
        }
    }
}