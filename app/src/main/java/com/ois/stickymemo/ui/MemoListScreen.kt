package com.ois.stickymemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import java.util.*
import com.ois.stickymemo.ui.parseChecklist

// 웜톤 컬러 팔레트
val WarmCream = Color(0xFFFFF8F0)
val WarmOrange = Color(0xFFFF8C42)
val WarmOrangeDark = Color(0xFFE67835)
val WarmYellow = Color(0xFFFFE082)
val WarmYellowLight = Color(0xFFFFF3C4)
val WarmPeach = Color(0xFFFFD4A8)
val WarmGreen = Color(0xFFB8E6B0)
val WarmBlue = Color(0xFFB8D4F0)
val WarmPink = Color(0xFFFFB8C0)
val WarmPurple = Color(0xFFE0B8F0)
val WarmBrown = Color(0xFF3E2723)
val WarmBrownLight = Color(0xFF795548)
val WarmBrownMid = Color(0xFF5D4037)

val StickyYellow = WarmYellow
val StickyYellowDark = WarmOrange
val StickyGreen = WarmGreen
val StickyBlue = WarmBlue
val StickyPink = WarmPink

fun memoColor(type: MemoType): Color = when (type) {
    MemoType.NORMAL -> WarmYellow
    MemoType.CHECKLIST -> WarmGreen
    MemoType.LOCATION -> WarmBlue
    MemoType.CALL -> WarmPink
}

enum class TimeGroup {
    TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, OLDER
}

fun getTimeGroup(timestamp: Long): TimeGroup {
    val now = Calendar.getInstance()
    val memoDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    if (now.get(Calendar.YEAR) == memoDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == memoDate.get(Calendar.DAY_OF_YEAR)
    ) return TimeGroup.TODAY

    val weekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            add(Calendar.DAY_OF_YEAR, -6)
        }
    }
    val weekEnd = Calendar.getInstance().apply {
        timeInMillis = weekStart.timeInMillis
        add(Calendar.DAY_OF_YEAR, 5)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }
    if (memoDate.after(weekStart) && memoDate.before(weekEnd)) return TimeGroup.THIS_WEEK

    if (now.get(Calendar.YEAR) == memoDate.get(Calendar.YEAR) &&
        now.get(Calendar.MONTH) == memoDate.get(Calendar.MONTH)
    ) return TimeGroup.THIS_MONTH

    val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    if (lastMonth.get(Calendar.YEAR) == memoDate.get(Calendar.YEAR) &&
        lastMonth.get(Calendar.MONTH) == memoDate.get(Calendar.MONTH)
    ) return TimeGroup.LAST_MONTH

    return TimeGroup.OLDER
}

fun timeGroupLabel(group: TimeGroup, context: Calendar = Calendar.getInstance()): String {
    return when (group) {
        TimeGroup.TODAY -> "오늘"
        TimeGroup.THIS_WEEK -> {
            val weekStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    add(Calendar.DAY_OF_YEAR, -6)
                }
            }
            val weekEnd = Calendar.getInstance().apply {
                timeInMillis = weekStart.timeInMillis
                add(Calendar.DAY_OF_YEAR, 5)
            }
            "${weekStart.get(Calendar.MONTH) + 1}월 ${weekStart.get(Calendar.DAY_OF_MONTH)}일 " +
                    "~ ${weekEnd.get(Calendar.MONTH) + 1}월 ${weekEnd.get(Calendar.DAY_OF_MONTH)}일"
        }
        TimeGroup.THIS_MONTH -> "${context.get(Calendar.MONTH) + 1}월"
        TimeGroup.LAST_MONTH -> {
            val last = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            "${last.get(Calendar.MONTH) + 1}월"
        }
        TimeGroup.OLDER -> "오래된 메모"
    }
}

fun memoTypeLabel(type: MemoType): String = when (type) {
    MemoType.NORMAL -> "📝 일반 메모"
    MemoType.CHECKLIST -> "📋 체크리스트"
    MemoType.LOCATION -> "📍 위치 메모"
    MemoType.CALL -> "📞 통화 메모"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    memos: List<Memo>,
    isDarkTheme: Boolean = false,
    showFab: Boolean = true,
    onToggleDarkTheme: () -> Unit = {},
    onAddMemo: (MemoType) -> Unit,
    onMemoClick: (Memo) -> Unit,
    onDeleteMemo: (Memo) -> Unit,
    onTogglePin: (Memo) -> Unit,
    onDuplicateMemo: (Memo) -> Unit
) {
    var showFab by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // 다크모드 색상 분기
    val bgColor = if (isDarkTheme) Color(0xFF121212) else WarmCream
    val topBarColor = if (isDarkTheme) Color(0xFF1E1E1E) else WarmOrange
    val topBarContentColor = if (isDarkTheme) Color(0xFFFF8C42) else Color.White
    val cardBgColor = if (isDarkTheme) Color(0xFF2C2C2C) else WarmYellow
    val textPrimary = if (isDarkTheme) Color(0xFFE0E0E0) else WarmBrown
    val textSecondary = if (isDarkTheme) Color(0xFFBDBDBD) else WarmBrownLight
    val tabBgColor = if (isDarkTheme) Color(0xFF1E1E1E) else WarmOrange
    val tabContentColor = if (isDarkTheme) Color(0xFFFF8C42) else Color.White
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val tabs = listOf("전체", "메모", "체크리스트", "통화")

    val tabFilteredMemos = when (selectedTab) {
        1 -> memos.filter { it.type == MemoType.NORMAL }
        2 -> memos.filter { it.type == MemoType.CHECKLIST }
        3 -> memos.filter { it.type == MemoType.CALL }
        else -> memos
    }

    val searchFilteredMemos = if (searchText.isBlank()) {
        tabFilteredMemos
    } else {
        tabFilteredMemos.filter { memo ->
            memo.title.contains(searchText, ignoreCase = true) ||
                    memo.content.contains(searchText, ignoreCase = true) ||
                    memo.contactName?.contains(searchText, ignoreCase = true) == true ||
                    memo.locationName?.contains(searchText, ignoreCase = true) == true
        }
    }

    val pinnedMemos = searchFilteredMemos.filter { it.isPinned }
    val unpinnedMemos = searchFilteredMemos.filter { !it.isPinned }

    val timeGroups = unpinnedMemos
        .groupBy { getTimeGroup(it.updatedAt) }
        .toSortedMap(compareBy { it.ordinal })

    val groupedMemos = timeGroups.mapValues { (_, memoList) ->
        memoList.groupBy { it.type }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("검색...", color = textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                                focusedTextColor = WarmBrown,
                                unfocusedTextColor = WarmBrown,
                                focusedBorderColor = WarmOrangeDark,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            "📝 StickyMemo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = topBarContentColor
                        )
                    }
                },
                actions = {
                    // 다크모드 토글 버튼
                    TextButton(onClick = onToggleDarkTheme) {
                        Text(
                            if (isDarkTheme) "🌞" else "🌙",
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchText = ""
                    }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "검색",
                            tint = topBarContentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                )
            )
        },
        containerColor = bgColor,
        floatingActionButton = {
            if (showFab) FloatingActionButton(
                onClick = {
                    val type = when (selectedTab) {
                        2 -> MemoType.CHECKLIST
                        3 -> MemoType.CALL
                        else -> MemoType.NORMAL  // 전체(0), 메모(1) 탭은 일반 메모
                    }
                    onAddMemo(type)
                },
                containerColor = WarmOrange,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "메모 추가",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = tabBgColor,
                contentColor = tabContentColor,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            if (searchFilteredMemos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "메모가 없습니다",
                            color = WarmBrownLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "+ 버튼으로 추가해보세요",
                            color = WarmBrownLight.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pinnedMemos.isNotEmpty()) {
                        item {
                            GroupHeader(
                                title = "⭐ 즐겨찾기",
                                count = pinnedMemos.size,
                                isExpanded = true,
                                onToggle = {},
                                isDarkTheme = isDarkTheme
                            )
                        }
                        items(pinnedMemos, key = { it.id }) { memo ->
                            MemoCard(
                                memo = memo,
                                onClick = { onMemoClick(memo) },
                                onDelete = { onDeleteMemo(memo) },
                                onTogglePin = { onTogglePin(memo) },
                                onDuplicate = { onDuplicateMemo(memo) },
                                onShare = { shareMemo(context, memo) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    groupedMemos.forEach { (timeGroup, typeMap) ->
                        item(key = "header_${timeGroup.name}") {
                            TimeGroupSection(
                                timeGroup = timeGroup,
                                typeMap = typeMap,
                                onMemoClick = onMemoClick,
                                onDeleteMemo = onDeleteMemo,
                                onTogglePin = onTogglePin,
                                onDuplicateMemo = onDuplicateMemo,
                                isDarkTheme = isDarkTheme,
                                onShareMemo = { memo -> shareMemo(context, memo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeGroupSection(
    timeGroup: TimeGroup,
    typeMap: Map<MemoType, List<Memo>>,
    onMemoClick: (Memo) -> Unit,
    onDeleteMemo: (Memo) -> Unit,
    onTogglePin: (Memo) -> Unit,
    onDuplicateMemo: (Memo) -> Unit,
    isDarkTheme: Boolean = false,
    onShareMemo: (Memo) -> Unit = {}
) {
    var isTimeGroupExpanded by remember { mutableStateOf(true) }
    val totalCount = typeMap.values.sumOf { it.size }

    Column {
        GroupHeader(
            title = timeGroupLabel(timeGroup),
            count = totalCount,
            isExpanded = isTimeGroupExpanded,
            onToggle = { isTimeGroupExpanded = !isTimeGroupExpanded },
            isTopLevel = true,
            isDarkTheme = isDarkTheme
        )

        AnimatedVisibility(
            visible = isTimeGroupExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                typeMap.forEach { (type, memoList) ->
                    var isTypeExpanded by remember { mutableStateOf(true) }

                    GroupHeader(
                        title = memoTypeLabel(type),
                        count = memoList.size,
                        isExpanded = isTypeExpanded,
                        onToggle = { isTypeExpanded = !isTypeExpanded },
                        isTopLevel = false,
                        isDarkTheme = isDarkTheme
                    )

                    AnimatedVisibility(
                        visible = isTypeExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            memoList.forEach { memo ->
                                MemoCard(
                                    memo = memo,
                                    onClick = { onMemoClick(memo) },
                                    onDelete = { onDeleteMemo(memo) },
                                    onTogglePin = { onTogglePin(memo) },
                                    onDuplicate = { onDuplicateMemo(memo) },
                                    onShare = { onShareMemo(memo) },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun GroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isTopLevel: Boolean = true,
    isDarkTheme: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .background(
                if (isDarkTheme) {
                    if (isTopLevel) Color(0xFF2A2A2A) else Color(0xFF242424)
                } else {
                    if (isTopLevel) WarmOrange.copy(alpha = 0.15f) else WarmPeach.copy(alpha = 0.4f)
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontWeight = if (isTopLevel) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (isTopLevel) 15.sp else 13.sp,
                color = if (isDarkTheme) Color(0xFFE0E0E0) else if (isTopLevel) WarmBrown else WarmBrownMid
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarmOrange)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Icon(
            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = WarmBrownLight,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FabMenuItem(label: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .background(color)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = WarmBrown,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onShare: () -> Unit = {},
    isDarkTheme: Boolean = false
){
    var isPinAnimating by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPinAnimating) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { isPinAnimating = false },
        label = "pin_scale"
    )

    val cardColor = if (isDarkTheme) Color(0xFF2C2C2C) else hexToColor(memo.colorHex)
    val textPrimary = if (isDarkTheme) Color(0xFFE0E0E0) else WarmBrown
    val textSecondary = if (isDarkTheme) Color(0xFFBDBDBD) else WarmBrownLight
    val textMid = if (isDarkTheme) Color(0xFFBDBDBD) else WarmBrownMid

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = WarmCream
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    memo.title.ifEmpty { "제목 없음" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WarmBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = WarmOrange.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            isPinAnimating = true
                            onTogglePin()
                            showBottomSheet = false
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (memo.isPinned) "⭐" else "☆", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        if (memo.isPinned) "즐겨찾기 해제" else "즐겨찾기 추가",
                        fontSize = 15.sp,
                        color = WarmBrown
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDuplicate()
                            showBottomSheet = false
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("메모 복제", fontSize = 15.sp, color = WarmBrown)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onShare()
                            showBottomSheet = false
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔗", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("메모 공유", fontSize = 15.sp, color = WarmBrown)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    showBottomSheet = true
                }
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (memo.isPinned) {
                        Text("⭐ ", fontSize = 14.sp)
                    }
                    Text(
                        text = memoTypeIcon(memo.type) + " " + memo.title.ifEmpty { "제목 없음" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textPrimary
                    )
                }
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "삭제",
                        tint = WarmBrownLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (memo.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = memo.content,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textMid
                )
            }
// 체크리스트 진행률 (카드 미리보기)
            if (memo.type == MemoType.CHECKLIST && !memo.checklistJson.isNullOrBlank()) {
                val items = parseChecklist(memo.checklistJson)
                if (items.isNotEmpty()) {
                    val checked = items.count { it.isChecked }
                    val total = items.size
                    val progress = checked.toFloat() / total.toFloat()

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (progress >= 1f) Color(0xFF66BB6A) else WarmOrange,
                            trackColor = WarmOrange.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$checked/$total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progress >= 1f) Color(0xFF388E3C) else WarmBrownLight
                        )
                    }
                    if (progress >= 1f) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "🎉 완료",
                            fontSize = 11.sp,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (memo.locationName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍 ", fontSize = 11.sp)
                    Text(
                        memo.locationName,
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }

            if (memo.type == MemoType.CALL && memo.contactName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📞 ", fontSize = 11.sp)
                    Text(
                        memo.contactName,
                        fontSize = 11.sp,
                        color = Color(0xFF880E4F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = WarmBrown.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "✍️ ${formatDateTime(memo.createdAt)}",
                    fontSize = 10.sp,
                    color = textSecondary
                )
                if (memo.updatedAt != memo.createdAt) {
                    Text(
                        "🔄 ${formatDateTime(memo.updatedAt)}",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM.dd HH:mm", java.util.Locale.KOREA)
    return sdf.format(java.util.Date(timestamp))
}
fun shareMemo(context: android.content.Context, memo: Memo) {
    val shareText = buildString {
        if (memo.title.isNotBlank()) {
            append(memo.title)
            append("\n\n")
        }
        when (memo.type) {
            MemoType.CHECKLIST -> {
                if (!memo.checklistJson.isNullOrBlank()) {
                    val items = parseChecklist(memo.checklistJson)
                    items.forEach { item ->
                        append(if (item.isChecked) "✅ " else "☐ ")
                        append(item.text)
                        append("\n")
                    }
                }
            }
            else -> {
                if (memo.content.isNotBlank()) append(memo.content)
            }
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, shareText.trim())
    }
    context.startActivity(android.content.Intent.createChooser(intent, "메모 공유"))
}
fun memoTypeIcon(type: MemoType): String = when (type) {
    MemoType.NORMAL -> "📝"
    MemoType.CHECKLIST -> "📋"
    MemoType.LOCATION -> "📍"
    MemoType.CALL -> "📞"
}
