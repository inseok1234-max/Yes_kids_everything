package com.ois.stickymemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            TopAppBar(
                title = {
                    Column {
                        Text("StickyMemo", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "생각나는 순간 바로 붙이는 기록",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(StickySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(StickySpacing.lg)
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
                item {
                    SectionHeader(
                        title = "고정한 기록",
                        subtitle = "자주 꺼내 보는 기록"
                    )
                }
                items(pinnedRecords, key = { "pinned_${it.id}" }) { record ->
                    HomeRecordRow(
                        record = record,
                        emphasized = true,
                        onClick = { openRecord(record, onMemoClick, onPlaceClick) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "최근 기록",
                    subtitle = "가장 최근에 붙잡은 생각과 장소"
                )
            }
            if (recentRecords.isEmpty()) {
                item {
                    StickyEmptyState(
                        icon = Icons.Default.StickyNote2,
                        title = "아직 기록이 없습니다",
                        message = "떠오른 생각을 짧게 남기면 여기에 다시 보여드릴게요.",
                        actionLabel = "빠른 메모 열기",
                        onAction = onQuickMemo
                    )
                }
            } else {
                items(recentRecords, key = { "recent_${it.id}" }) { record ->
                    HomeRecordRow(
                        record = record,
                        onClick = { openRecord(record, onMemoClick, onPlaceClick) }
                    )
                }
            }

            if (todayChecklist.isNotEmpty()) {
                item { SectionHeader(title = "오늘 체크할 일") }
                items(todayChecklist, key = { "task_${it.id}" }) { record ->
                    HomeRecordRow(record = record, onClick = { openRecord(record, onMemoClick, onPlaceClick) })
                }
            }

            if (recentPlaces.isNotEmpty()) {
                item { SectionHeader(title = "최근 장소", subtitle = "다시 가고 싶은 곳") }
                items(recentPlaces, key = { "place_${it.id}" }) { record ->
                    HomeRecordRow(record = record, onClick = { openRecord(record, onMemoClick, onPlaceClick) })
                }
            }

            item { Spacer(modifier = Modifier.height(StickySpacing.xxl)) }
        }
    }
}

@Composable
private fun QuickCaptureCard(onQuickMemo: () -> Unit) {
    StickyCard(contentPadding = StickySpacing.xl) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("빠른 기록", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onQuickMemo) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("메모")
            }
        }
        Spacer(modifier = Modifier.height(StickySpacing.sm))
        Text(
            "짧게 적고 바로 저장하세요. 정리는 나중에 해도 괜찮습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeRecordRow(
    record: RecordCardUiModel,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(StickyRadius.card),
        tonalElevation = if (emphasized) StickyElevation.floating else StickyElevation.card,
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.surface
    ) {
        ListItem(
            headlineContent = {
                Text(record.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(record.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                Icon(
                    if (record.isPinned) Icons.Default.PushPin else record.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        record.meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        record.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
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
