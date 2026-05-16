package com.ois.stickymemo.data

class MemoRepository(private val dao: MemoDao) {
    val allMemos = dao.getAllMemos()
    val checklistMemos = dao.getMemosByType(MemoType.CHECKLIST)
    val locationMemos = dao.getMemosByType(MemoType.LOCATION)
    val callMemos = dao.getMemosByType(MemoType.CALL)

    suspend fun getMemoById(id: Int): Memo? = dao.getMemoById(id)

    suspend fun insertMemo(memo: Memo): Long = dao.insertMemo(memo)

    suspend fun updateMemo(memo: Memo) {
        dao.updateMemo(memo)
    }

    suspend fun deleteMemo(memo: Memo) {
        dao.deleteMemo(memo)
    }

    suspend fun deleteCompletedMemos() {
        dao.deleteCompletedMemos()
    }

    suspend fun togglePin(memo: Memo) {
        dao.updateMemo(memo.copy(isPinned = !memo.isPinned))
    }

    suspend fun duplicateMemo(memo: Memo) {
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
