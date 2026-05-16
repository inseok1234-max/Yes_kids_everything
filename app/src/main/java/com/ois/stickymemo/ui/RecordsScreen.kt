package com.ois.stickymemo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    memos: List<Memo>,
    places: List<Restaurant>,
    onRecordClick: (RecordCardUiModel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf<RecordKind?>(null) }
    val records = remember(memos, places) {
        (memos.map { it.toRecordCardUiModel() } + places.map { it.toRecordCardUiModel() })
            .sortedByDescending { it.timestamp }
    }
    val filteredRecords = records.filter { record ->
        val matchesKind = selectedKind == null || record.kind == selectedKind
        val matchesQuery = query.isBlank() ||
            record.title.contains(query, ignoreCase = true) ||
            record.subtitle.contains(query, ignoreCase = true) ||
            record.meta.contains(query, ignoreCase = true)
        matchesKind && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("기록", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "모든 기록을 빠르게 찾기",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = StickySpacing.lg, vertical = StickySpacing.md),
                verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(StickyRadius.card)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedKind == null,
                        onClick = { selectedKind = null },
                        label = { Text("전체") }
                    )
                    listOf(
                        RecordKind.MEMO,
                        RecordKind.TASK,
                        RecordKind.PLACE,
                        RecordKind.REMINDER,
                        RecordKind.CALL
                    ).forEach { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = if (selectedKind == kind) null else kind },
                            label = { Text(kind.displayName) }
                        )
                    }
                }
            }

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(StickySpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    StickyEmptyState(
                        icon = Icons.Default.StickyNote2,
                        title = if (query.isBlank()) "아직 기록이 없습니다" else "검색 결과가 없습니다",
                        message = if (query.isBlank()) {
                            "오른쪽 아래 + 버튼으로 메모, 장소, 체크리스트를 추가해보세요."
                        } else {
                            "다른 단어나 필터로 다시 찾아보세요."
                        },
                        actionLabel = "확인",
                        onAction = {}
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = StickySpacing.lg, vertical = StickySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        UnifiedRecordCard(
                            record = record,
                            onClick = { onRecordClick(record) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UnifiedRecordCard(
    record: RecordCardUiModel,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (record.isPinned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "record_card_color"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(StickyRadius.card),
        tonalElevation = StickyElevation.card,
        shadowElevation = StickyElevation.card,
        color = containerColor
    ) {
        ListItem(
            headlineContent = {
                AnimatedContent(targetState = record.title, label = "record_title") { title ->
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            supportingContent = {
                Text(record.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                Icon(record.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
