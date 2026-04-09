package com.ois.stickymemo.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ois.stickymemo.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MemoDatabase.getDatabase(application).memoDao()

    // 전체 메모 목록
    val allMemos = dao.getAllMemos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 타입별 메모
    val checklistMemos = dao.getMemosByType(MemoType.CHECKLIST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationMemos = dao.getMemosByType(MemoType.LOCATION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callMemos = dao.getMemosByType(MemoType.CALL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 메모 저장
    fun insertMemo(memo: Memo) = viewModelScope.launch {
        dao.insertMemo(memo)
    }

    // 메모 수정
    fun updateMemo(memo: Memo) = viewModelScope.launch {
        dao.updateMemo(memo)
    }

    // 메모 삭제
    fun deleteMemo(memo: Memo) = viewModelScope.launch {
        dao.deleteMemo(memo)
    }

    // 완료 메모 전체 삭제
    fun deleteCompletedMemos() = viewModelScope.launch {
        dao.deleteCompletedMemos()
    }

    // 즐겨찾기 토글
    fun togglePin(memo: Memo) = viewModelScope.launch {
        dao.updateMemo(memo.copy(isPinned = !memo.isPinned))
    }

    // 메모 복제
    fun duplicateMemo(memo: Memo) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        dao.insertMemo(
            memo.copy(
                id = 0,
                title = memo.title + " (복사)",
                createdAt = now,
                updatedAt = now,
                isPinned = false
            )
        )
    }
}

class MemoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}