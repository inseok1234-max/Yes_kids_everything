package com.ois.stickymemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    memos: List<Memo>,
    places: List<Restaurant>,
    onQuickMemo: () -> Unit,
    onMemoClick: (Memo) -> Unit,
    onPlaceClick: (Restaurant) -> Unit
) {
    val allRecords = remember(memos, places) {
        (memos.map { it.toRecordCardUiModel() } + places.map { it.toRecordCardUiModel() })
            .sortedByDescending { it.timestamp }
    }
    val pinnedRecords = allRecords.filter { it.isPinned }.take(3)
    val recentRecords = allRecords.take(6)
    val todayChecklist = allRecords.filter { it.kind == RecordKind.TASK }.take(3)
    val recentPlaces = allRecords.filter { it.kind == RecordKind.PLACE }.take(3)

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "StickyMemo",
                subtitle = "바로 쓰고 다시 찾는 시작 화면"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(StickyLayout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(StickyLayout.sectionGap)
        ) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })
                ) {
                    QuickCaptureCard(onQuickMemo = onQuickMemo)
                }
            }

            if (pinnedRecords.isNotEmpty()) {
                item { StickySectionHeader(title = "고정 기록", subtitle = "자주 다시 보는 기록") }
                items(pinnedRecords, key = { "pinned_${it.id}" }) { record ->
                    TimelineRecordRow(
                        record = record,
                        emphasized = true,
                        onClick = { openRecord(record, onMemoClick, onPlaceClick) }
                    )
                }
            }

            item { StickySectionHeader(title = "최근 기록", subtitle = "방금 남긴 생각과 할 일") }
            if (recentRecords.isEmpty()) {
                item {
                    StickyEmptyState(
                        icon = Icons.Default.StickyNote2,
                        title = "아직 기록이 없습니다",
                        message = "첫 메모를 남기면 여기서 바로 다시 볼 수 있습니다.",
                        actionLabel = "첫 메모 남기기",
                        onAction = onQuickMemo
                    )
                }
            } else {
                items(recentRecords, key = { "recent_${it.id}" }) { record ->
                    TimelineRecordRow(
                        record = record,
                        onClick = { openRecord(record, onMemoClick, onPlaceClick) }
                    )
                }
            }

            if (todayChecklist.isNotEmpty()) {
                item { StickySectionHeader(title = "오늘 체크할 일") }
                items(todayChecklist, key = { "task_${it.id}" }) { record ->
                    TimelineRecordRow(record = record, onClick = { openRecord(record, onMemoClick, onPlaceClick) })
                }
            }

            if (recentPlaces.isNotEmpty()) {
                item { StickySectionHeader(title = "최근 장소", subtitle = "다시 찾기 쉬운 장소 기록") }
                items(recentPlaces, key = { "place_${it.id}" }) { record ->
                    CompactPlaceRow(record = record, onClick = { openRecord(record, onMemoClick, onPlaceClick) })
                }
            }

            item { Spacer(modifier = Modifier.height(StickySpacing.xxl)) }
        }
    }
}

@Composable
private fun QuickCaptureCard(onQuickMemo: () -> Unit) {
    StickySoftCard(contentPadding = StickySpacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StickySpacing.md)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.padding(StickySpacing.sm),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("빠른 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "생각난 순간 바로 남기세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(StickySpacing.md))
        StickyPrimaryButton(
            label = "바로 메모 쓰기",
            onClick = onQuickMemo,
            icon = Icons.Default.Add
        )
    }
}

@Composable
private fun TimelineRecordRow(
    record: RecordCardUiModel,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        tonalElevation = StickyElevation.flat,
        shadowElevation = StickyElevation.flat
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = StickySpacing.md, vertical = StickySpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(StickySpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (record.isPinned) Icons.Default.PushPin else record.icon,
                    contentDescription = null,
                    modifier = Modifier.size(StickyIconSize.sm),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(StickySpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            record.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            record.meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        record.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        record.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StickyDivider(modifier = Modifier.padding(start = 44.dp))
        }
    }
}

@Composable
private fun CompactPlaceRow(
    record: RecordCardUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(StickyRadius.largeCard),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = StickyElevation.card
    ) {
        Row(
            modifier = Modifier.padding(StickySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    record.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                record.dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openRecord(
    record: RecordCardUiModel,
    onMemoClick: (Memo) -> Unit,
    onPlaceClick: (Restaurant) -> Unit
) {
    when (val source = record.source) {
        is RecordSource.MemoRecord -> onMemoClick(source.memo)
        is RecordSource.PlaceRecord -> onPlaceClick(source.place)
    }
}
