package com.ois.stickymemo.ui

import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Contact(
    val name: String,
    val phone: String
)

fun getContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameIndex) ?: continue
            val phone = it.getString(phoneIndex) ?: continue
            contacts.add(Contact(name = name, phone = phone))
        }
    }
    return contacts
}

@Composable
fun ContactPickerDialog(
    context: Context,
    onContactSelected: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    val contacts = remember { getContacts(context) }
    var searchText by remember { mutableStateOf("") }
    val filtered = contacts.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.phone.contains(searchText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("연락처 선택", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("검색") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(filtered) { contact ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSelected(contact) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                contact.name,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                contact.phone,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}