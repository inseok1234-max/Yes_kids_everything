package com.ois.stickymemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ois.stickymemo.data.Restaurant
import com.ois.stickymemo.viewmodel.RestaurantSortOrder
import com.ois.stickymemo.viewmodel.RestaurantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListScreen(
    viewModel: RestaurantViewModel,
    showFab: Boolean = true,
    onAddClick: () -> Unit,
    onItemClick: (Restaurant) -> Unit
) {
    val restaurants by viewModel.restaurants.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("장소", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "다시 찾고 싶은 장소들",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "장소 추가")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PlacesFilterBar(
                allTags = allTags,
                sortOrder = sortOrder,
                selectedTag = selectedTag,
                onSortSelected = viewModel::setSortOrder,
                onTagSelected = viewModel::setTagFilter
            )

            if (restaurants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(StickySpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    StickyEmptyState(
                        icon = Icons.Default.Place,
                        title = "저장한 장소가 없습니다",
                        message = "카페, 병원, 여행지, 거래처처럼 다시 찾고 싶은 장소를 남겨보세요.",
                        actionLabel = "장소 기록하기",
                        onAction = onAddClick
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(StickySpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(StickySpacing.lg)
                ) {
                    items(restaurants, key = { it.id }) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            onClick = { onItemClick(restaurant) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesFilterBar(
    allTags: List<String>,
    sortOrder: RestaurantSortOrder,
    selectedTag: String?,
    onSortSelected: (RestaurantSortOrder) -> Unit,
    onTagSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = StickySpacing.lg, vertical = StickySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sortOrder == RestaurantSortOrder.LATEST,
                onClick = { onSortSelected(RestaurantSortOrder.LATEST) },
                label = { Text("최근 방문") }
            )
            FilterChip(
                selected = sortOrder == RestaurantSortOrder.RATING,
                onClick = { onSortSelected(RestaurantSortOrder.RATING) },
                label = { Text("평점 높은 순") },
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(StickyIconSize.sm))
                }
            )
            FilterChip(
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
                label = { Text("전체") }
            )
            allTags.forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagSelected(if (selectedTag == tag) null else tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    val firstImage = restaurant.imageUris.split(",").firstOrNull { it.isNotBlank() }
    StickyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.7f)
                .clip(RoundedCornerShape(topStart = StickyRadius.card, topEnd = StickyRadius.card))
        ) {
            if (firstImage != null) {
                AsyncImage(
                    model = firstImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(StickyIconSize.empty),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
            RatingBadge(
                rating = restaurant.rating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(StickySpacing.md)
            )
        }

        Column(
            modifier = Modifier.padding(StickySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
        ) {
            Text(
                restaurant.name.ifBlank { "장소 이름 없음" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (restaurant.location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(StickyIconSize.sm),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        restaurant.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (restaurant.review.isNotBlank()) {
                Text(
                    restaurant.review,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val tags = restaurant.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs)
                ) {
                    tags.take(8).forEach { tag ->
                        RecordTypeChip(label = "#$tag")
                    }
                }
            }
            Text(
                "방문 ${formatVisitedDate(restaurant.visitedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(StickyRadius.chip),
        color = Color(0xFFFFF0C2),
        tonalElevation = StickyElevation.floating
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StickySpacing.sm, vertical = StickySpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(StickyIconSize.sm),
                tint = Color(0xFFE0A100)
            )
            Text(
                String.format(Locale.KOREA, "%.1f", rating),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4D3900)
            )
        }
    }
}

private fun formatVisitedDate(timestamp: Long): String {
    return SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp))
}
