package com.ois.stickymemo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    // 전체 메모 불러오기 (최신순)
    @Query("SELECT * FROM memos ORDER BY createdAt DESC")
    fun getAllMemos(): Flow<List<Memo>>

    // 타입별 메모 불러오기
    @Query("SELECT * FROM memos WHERE type = :type ORDER BY createdAt DESC")
    fun getMemosByType(type: MemoType): Flow<List<Memo>>

    // 위치 메모만 불러오기 (Geofencing용)
    @Query("SELECT * FROM memos WHERE type = 'LOCATION' AND latitude IS NOT NULL")
    fun getLocationMemos(): Flow<List<Memo>>

    // 메모 단건 조회
    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemoById(id: Int): Memo?

    // 메모 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: Memo): Long

    // 메모 수정
    @Update
    suspend fun updateMemo(memo: Memo)

    // 메모 삭제
    @Delete
    suspend fun deleteMemo(memo: Memo)

    // 완료된 메모 삭제
    @Query("DELETE FROM memos WHERE isCompleted = 1")
    suspend fun deleteCompletedMemos()
}