package com.ois.stickymemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoDatabase
import com.ois.stickymemo.data.MemoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemoRepository(
        MemoDatabase.getDatabase(application).memoDao()
    )

    val allMemos = repository.allMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checklistMemos = repository.checklistMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationMemos = repository.locationMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callMemos = repository.callMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertMemo(memo: Memo) = viewModelScope.launch {
        repository.insertMemo(memo)
    }

    fun updateMemo(memo: Memo) = viewModelScope.launch {
        repository.updateMemo(memo)
    }

    fun deleteMemo(memo: Memo) = viewModelScope.launch {
        repository.deleteMemo(memo)
    }

    fun deleteCompletedMemos() = viewModelScope.launch {
        repository.deleteCompletedMemos()
    }

    fun togglePin(memo: Memo) = viewModelScope.launch {
        repository.togglePin(memo)
    }

    fun duplicateMemo(memo: Memo) = viewModelScope.launch {
        repository.duplicateMemo(memo)
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
