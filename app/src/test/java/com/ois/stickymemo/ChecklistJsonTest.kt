package com.ois.stickymemo

import com.ois.stickymemo.data.ChecklistItem
import com.ois.stickymemo.ui.checklistToJson
import com.ois.stickymemo.ui.parseChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistJsonTest {
    @Test
    fun checklist_roundTrip_preservesItems() {
        val items = listOf(
            ChecklistItem(id = 1, text = "milk", isChecked = false),
            ChecklistItem(id = 2, text = "eggs", isChecked = true)
        )

        val parsed = parseChecklist(checklistToJson(items))

        assertEquals(items, parsed)
    }

    @Test
    fun parseChecklist_returnsEmptyListForInvalidJson() {
        assertTrue(parseChecklist("not-json").isEmpty())
    }
}
