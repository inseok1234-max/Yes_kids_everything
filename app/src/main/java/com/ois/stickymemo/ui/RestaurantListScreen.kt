package com.ois.stickymemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var query by remember { mutableStateOf("") }

    val visiblePlaces = restaurants.filter { place ->
        query.isBlank() ||
            place.name.contains(query, ignoreCase = true) ||
            place.location.contains(query, ignoreCase = true) ||
            place.review.contains(query, ignoreCase = true) ||
            place.tags.contains(query, ignoreCase = true) ||
            place.recipeTitle.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "장소",
                subtitle = "다시 찾고 싶은 곳 ${restaurants.size}개"
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
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
                query = query,
                onQueryChange = { query = it },
                visibleCount = visiblePlaces.size,
                allTags = allTags,
                sortOrder = sortOrder,
                selectedTag = selectedTag,
                onSortSelected = viewModel::setSortOrder,
                onTagSelected = viewModel::setTagFilter
            )

            if (visiblePlaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(StickyLayout.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    StickyEmptyState(
                        icon = Icons.Default.Place,
                        title = if (restaurants.isEmpty()) "저장한 장소가 없습니다" else "맞는 장소가 없습니다",
                        message = if (restaurants.isEmpty()) {
                            "카페, 병원, 여행지처럼 다시 찾고 싶은 장소를 기록해보세요."
                        } else {
                            "검색어나 태그를 바꿔 다시 찾아보세요."
                        },
                        actionLabel = "장소 기록하기",
                        onAction = onAddClick
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(StickyLayout.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
                ) {
                    items(visiblePlaces, key = { it.id }) { restaurant ->
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
    query: String,
    onQueryChange: (String) -> Unit,
    visibleCount: Int,
    allTags: List<String>,
    sortOrder: RestaurantSortOrder,
    selectedTag: String?,
    onSortSelected: (RestaurantSortOrder) -> Unit,
    onTagSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = StickyLayout.screenPadding, vertical = StickySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
    ) {
        StickySearchBar(
            value = query,
            onValueChange = onQueryChange,
            label = "장소명, 주소, 태그 검색",
            leadingIcon = Icons.Default.Search
        )
        Text(
            "${visibleCount}개 장소 표시",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StickyFilterChip(
                selected = sortOrder == RestaurantSortOrder.LATEST,
                onClick = { onSortSelected(RestaurantSortOrder.LATEST) },
                label = "최근 방문"
            )
            StickyFilterChip(
                selected = sortOrder == RestaurantSortOrder.RATING,
                onClick = { onSortSelected(RestaurantSortOrder.RATING) },
                label = "평점 높은 순",
                icon = Icons.Default.Star
            )
            StickyFilterChip(
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
                label = "전체"
            )
            allTags.forEach { tag ->
                StickyFilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagSelected(if (selectedTag == tag) null else tag) },
                    label = tag
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
    var imageLoadFailed by remember(firstImage) { mutableStateOf(false) }
    val tags = restaurant.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val fallbackMeta = restaurant.location.ifBlank {
        tags.take(2).joinToString("  ") { "#$it" }
    }

    StickySoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(StickySpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(StickyRadius.card))
            ) {
                if (firstImage != null && !imageLoadFailed) {
                    AsyncImage(
                        model = firstImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(StickySpacing.xs)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        restaurant.name.ifBlank { "장소 이름 없음" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "방문 ${formatVisitedDate(restaurant.visitedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (restaurant.recipeUrl.isNotBlank()) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(StickyIconSize.md),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingBadge(rating = restaurant.rating)
                    tags.firstOrNull()?.let { tag ->
                        Text(
                            "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = StickyElevation.flat,
        shadowElevation = StickyElevation.floating
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
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                String.format(Locale.KOREA, "%.1f", rating),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private fun formatVisitedDate(timestamp: Long): String {
    return SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp))
}
