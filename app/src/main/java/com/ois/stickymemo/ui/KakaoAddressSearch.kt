package com.ois.stickymemo.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ois.stickymemo.BuildConfig
import com.ois.stickymemo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

private const val KAKAO_SEARCH_TAG = "KakaoSearch"

data class AddressResult(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

suspend fun searchAddress(query: String): List<AddressResult> {
    val apiKey = BuildConfig.KAKAO_REST_API_KEY.trim()
    if (apiKey.isBlank()) {
        Log.w(KAKAO_SEARCH_TAG, "API key missing: BuildConfig.KAKAO_REST_API_KEY is blank.")
        return emptyList()
    }

    return withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://dapi.kakao.com/v2/local/search/keyword.json?query=$encodedQuery")
                .addHeader("Authorization", "KakaoAK $apiKey")
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                when (response.code) {
                    401, 403 -> {
                        Log.w(KAKAO_SEARCH_TAG, "Authorization failed: HTTP ${response.code}. Check Kakao REST API key.")
                        return@withContext emptyList()
                    }
                }

                if (!response.isSuccessful) {
                    Log.w(KAKAO_SEARCH_TAG, "Search failed: HTTP ${response.code}.")
                    return@withContext emptyList()
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    Log.d(KAKAO_SEARCH_TAG, "Search returned an empty response body.")
                    return@withContext emptyList()
                }

                val documents = try {
                    JSONObject(body).getJSONArray("documents")
                } catch (parseError: Exception) {
                    Log.w(KAKAO_SEARCH_TAG, "Parse failure while reading Kakao search response.", parseError)
                    return@withContext emptyList()
                }

                if (documents.length() == 0) {
                    Log.d(KAKAO_SEARCH_TAG, "Search completed with empty result.")
                }

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
            }
        } catch (networkError: IOException) {
            Log.w(KAKAO_SEARCH_TAG, "Network failure while calling Kakao local search.", networkError)
            emptyList()
        } catch (error: Exception) {
            Log.w(KAKAO_SEARCH_TAG, "Unexpected search failure.", error)
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
    val isSearchEnabled = BuildConfig.KAKAO_REST_API_KEY.isNotBlank()
    val disabledMessage = stringResource(R.string.address_search_disabled)
    val noResultsMessage = stringResource(R.string.address_search_no_results)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.address_search_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(stringResource(R.string.address_search_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (!isSearchEnabled) {
                                errorMsg = disabledMessage
                                return@IconButton
                            }
                            if (searchText.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    errorMsg = ""
                                    results = searchAddress(searchText)
                                    isLoading = false
                                    if (results.isEmpty()) errorMsg = noResultsMessage
                                }
                            }
                        },
                        enabled = searchText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                }

                if (!isSearchEnabled) {
                    Text(
                        disabledMessage,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                                    Text(result.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text(result.address, fontSize = 12.sp, color = Color.Gray)
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
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
