package com.ois.stickymemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoType {
    NORMAL,      // 일반 메모
    CHECKLIST,   // 장보기 체크리스트
    LOCATION,    // 위치 알림 메모
    CALL         // 통화 메모
}

@Entity(tableName = "memos")
data class Memo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: MemoType = MemoType.NORMAL,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // 위치 메모용
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val locationRadius: Float = 100f,

    // 통화 메모용
    val contactName: String? = null,
    val contactPhone: String? = null,

    // 체크리스트용 (JSON 문자열로 저장)
    val checklistJson: String? = null,

    val isCompleted: Boolean = false,
    val colorHex: String = "#FFF176",
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: String = "",
    val imageUri: String? = null
)

data class ChecklistItem(
    val id: Int,
    val text: String,
    val isChecked: Boolean = false
)