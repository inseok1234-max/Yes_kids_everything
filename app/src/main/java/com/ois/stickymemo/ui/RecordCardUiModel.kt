package com.ois.stickymemo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import com.ois.stickymemo.data.Restaurant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecordKind {
    MEMO,
    TASK,
    PLACE,
    REMINDER,
    CALL
}

sealed class RecordSource {
    data class MemoRecord(val memo: Memo) : RecordSource()
    data class PlaceRecord(val place: Restaurant) : RecordSource()
}

data class RecordCardUiModel(
    val id: String,
    val kind: RecordKind,
    val title: String,
    val subtitle: String,
    val meta: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val icon: ImageVector,
    val source: RecordSource
) {
    val dateLabel: String = SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp))
}

fun Memo.toRecordCardUiModel(): RecordCardUiModel {
    val kind = when (type) {
        MemoType.NORMAL -> RecordKind.MEMO
        MemoType.CHECKLIST -> RecordKind.TASK
        MemoType.LOCATION -> RecordKind.REMINDER
        MemoType.CALL -> RecordKind.CALL
    }
    val icon = when (type) {
        MemoType.NORMAL -> Icons.Default.StickyNote2
        MemoType.CHECKLIST -> Icons.Default.CheckBox
        MemoType.LOCATION -> Icons.Default.LocationOn
        MemoType.CALL -> Icons.Default.Call
    }
    return RecordCardUiModel(
        id = "memo_$id",
        kind = kind,
        title = title.ifBlank { "제목 없음" },
        subtitle = when {
            content.isNotBlank() -> content
            locationName?.isNotBlank() == true -> locationName
            contactName?.isNotBlank() == true -> contactName
            else -> kind.displayName
        },
        meta = kind.displayName,
        timestamp = updatedAt,
        isPinned = isPinned,
        icon = icon,
        source = RecordSource.MemoRecord(this)
    )
}

fun Restaurant.toRecordCardUiModel(): RecordCardUiModel {
    return RecordCardUiModel(
        id = "place_$id",
        kind = RecordKind.PLACE,
        title = name.ifBlank { "장소 이름 없음" },
        subtitle = location.ifBlank { review.ifBlank { "장소 기록" } },
        meta = "장소",
        timestamp = visitedAt,
        icon = Icons.Default.Place,
        source = RecordSource.PlaceRecord(this)
    )
}

val RecordKind.displayName: String
    get() = when (this) {
        RecordKind.MEMO -> "메모"
        RecordKind.TASK -> "체크리스트"
        RecordKind.PLACE -> "장소"
        RecordKind.REMINDER -> "위치 알림"
        RecordKind.CALL -> "통화 메모"
    }
