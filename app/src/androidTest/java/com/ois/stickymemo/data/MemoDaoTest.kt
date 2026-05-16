package com.ois.stickymemo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MemoDaoTest {
    private lateinit var database: MemoDatabase
    private lateinit var dao: MemoDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.memoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUpdateDeleteMemo() = runBlocking {
        val id = dao.insertMemo(Memo(title = "first", content = "body")).toInt()

        assertEquals("first", dao.getMemoById(id)?.title)

        dao.updateMemo(dao.getMemoById(id)!!.copy(title = "updated"))
        assertEquals("updated", dao.getMemoById(id)?.title)

        dao.deleteMemo(dao.getMemoById(id)!!)
        assertNull(dao.getMemoById(id))
    }

    @Test
    fun getMemosByType_filtersByMemoType() = runBlocking {
        dao.insertMemo(Memo(type = MemoType.NORMAL, title = "plain"))
        dao.insertMemo(Memo(type = MemoType.CHECKLIST, title = "checklist"))

        val checklist = dao.getMemosByType(MemoType.CHECKLIST).first()

        assertEquals(1, checklist.size)
        assertEquals("checklist", checklist.single().title)
    }
}
