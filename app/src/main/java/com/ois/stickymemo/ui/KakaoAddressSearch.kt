package com.ois.stickymemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AddressResult(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

suspend fun searchAddress(query: String): List<AddressResult> {
    return withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://dapi.kakao.com/v2/local/search/keyword.json?query=$encodedQuery")
                .addHeader("Authorization", "KakaoAK 8fcc428adfb5b5b874e6ea9723a40554")
                .build()
            val response = client.newCall(request).execute()
            val responseCode = response.code
            android.util.Log.d("KakaoSearch", "응답코드: $responseCode")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                android.util.Log.e("KakaoSearch", "에러: $errorBody")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            android.util.Log.d("KakaoSearch", "응답: $body")

            val json = JSONObject(body)
            val documents = json.getJSONArray("documents")

            List(documents.length()) { i ->
                val doc = documents.getJSONObject(i)
                AddressResult(
                    name = doc.getString("place_name"),
                    address = doc.optString("road_address_name").ifEmpty {
                        doc.getString("address_name")
                    },
                    lat = doc.getString("y").toDouble(),
                    lng = doc.getString("x").toDouble()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("KakaoSearch", "예외: ${e.message}", e)
            emptyList()
        }
    }
}

@Composable
fun AddressSearchDialog(
    onAddressSelected: (AddressResult) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AddressResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주소 검색", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("주소 또는 장소명 입력") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (searchText.isNotBlank()) {
                            scope.launch {
                                isLoading = true
                                errorMsg = ""
                                results = searchAddress(searchText)
                                isLoading = false
                                if (results.isEmpty()) errorMsg = "검색 결과가 없습니다"
                            }
                        }
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMsg.isNotEmpty() -> {
                        Text(errorMsg, color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(results) { result ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAddressSelected(result) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        result.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        result.address,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
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