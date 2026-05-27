package com.ois.stickymemo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    memos: List<Memo>,
    places: List<Restaurant>,
    onRecordClick: (RecordCardUiModel) -> Unit,
    onMemoDelete: (Memo) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf<RecordKind?>(null) }
    var pinnedOnly by remember { mutableStateOf(false) }
    var memoPendingDelete by remember { mutableStateOf<Memo?>(null) }
    val records = remember(memos, places) {
        (memos.map { it.toRecordCardUiModel() } + places.map { it.toRecordCardUiModel() })
            .sortedByDescending { it.timestamp }
    }

    memoPendingDelete?.let { memo ->
        AlertDialog(
            onDismissRequest = { memoPendingDelete = null },
            title = { Text("메모 삭제") },
            text = { Text("이 메모를 삭제할까요? 삭제한 메모는 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMemoDelete(memo)
                        memoPendingDelete = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { memoPendingDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
    val filteredRecords = records.filter { record ->
        val matchesKind = selectedKind == null || record.kind == selectedKind
        val matchesPinned = !pinnedOnly || record.isPinned
        val matchesQuery = query.isBlank() ||
            record.title.contains(query, ignoreCase = true) ||
            record.subtitle.contains(query, ignoreCase = true) ||
            record.meta.contains(query, ignoreCase = true) ||
            record.searchText.contains(query, ignoreCase = true)
        matchesKind && matchesPinned && matchesQuery
    }

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "기록",
                subtitle = "${records.size}개 기록에서 바로 찾기"
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
                modifier = Modifier.padding(horizontal = StickyLayout.screenPadding, vertical = StickySpacing.md),
                verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)
            ) {
                StickySearchBar(
                    value = query,
                    onValueChange = { query = it },
                    label = "제목, 내용, 태그, 장소 검색",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${filteredRecords.size}개 표시",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (query.isNotBlank() || selectedKind != null || pinnedOnly) {
                        Text(
                            "필터 초기화",
                            modifier = Modifier.clickable {
                                query = ""
                                selectedKind = null
                                pinnedOnly = false
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StickyFilterChip(
                        selected = selectedKind == null,
                        onClick = { selectedKind = null },
                        label = "전체"
                    )
                    StickyFilterChip(
                        selected = pinnedOnly,
                        onClick = { pinnedOnly = !pinnedOnly },
                        label = "고정",
                        icon = Icons.Default.PushPin
                    )
                    listOf(
                        RecordKind.MEMO,
                        RecordKind.TASK,
                        RecordKind.PLACE,
                        RecordKind.REMINDER,
                        RecordKind.CALL
                    ).forEach { kind ->
                        StickyFilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = if (selectedKind == kind) null else kind },
                            label = kind.displayName
                        )
                    }
                }
            }

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(StickyLayout.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    StickyEmptyState(
                        icon = Icons.Default.StickyNote2,
                        title = if (query.isBlank()) "아직 기록이 없습니다" else "검색 결과가 없습니다",
                        message = if (query.isBlank()) {
                            "오른쪽 아래 기록 버튼으로 메모, 장소, 체크리스트를 바로 추가해보세요."
                        } else {
                            "다른 단어나 필터로 다시 찾아보세요."
                        },
                        actionLabel = "필터 초기화",
                        onAction = {
                            query = ""
                            selectedKind = null
                            pinnedOnly = false
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = StickyLayout.screenPadding, vertical = StickySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        UnifiedRecordCard(
                            record = record,
                            onClick = { onRecordClick(record) },
                            onLongClick = {
                                val source = record.source
                                if (source is RecordSource.MemoRecord) {
                                    memoPendingDelete = source.memo
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun UnifiedRecordCard(
    record: RecordCardUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(0.dp),
        tonalElevation = StickyElevation.card,
        shadowElevation = StickyElevation.card,
        color = containerColor
    ) {
        Column {
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
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            record.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            StickyDivider(modifier = Modifier.padding(start = 56.dp))
        }
    }
}

private val RecordCardUiModel.searchText: String
    get() = when (val item = source) {
        is RecordSource.MemoRecord -> item.memo.tags
        is RecordSource.PlaceRecord -> listOf(
            item.place.tags,
            item.place.location,
            item.place.review,
            item.place.recipeTitle,
            item.place.recipeUrl
        ).joinToString(" ")
    }
