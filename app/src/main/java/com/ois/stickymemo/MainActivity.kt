package com.ois.stickymemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import com.ois.stickymemo.data.Restaurant
import com.ois.stickymemo.ui.*
import com.ois.stickymemo.ui.theme.StickyMemoTheme
import com.ois.stickymemo.viewmodel.MemoViewModel
import com.ois.stickymemo.viewmodel.MemoViewModelFactory
import com.ois.stickymemo.viewmodel.RestaurantViewModel
import com.ois.stickymemo.viewmodel.RestaurantViewModelFactory

// ── 탭 정의 ──
enum class MainTab { MEMO, RESTAURANT }

// ── 메모 화면 네비게이션 ──
sealed class Screen {
    object List : Screen()
    data class Edit(val memo: Memo? = null, val type: MemoType = MemoType.NORMAL) : Screen()
}

// ── 맛집 화면 네비게이션 ──
sealed class RestaurantScreen {
    object List : RestaurantScreen()
    data class Edit(val restaurant: Restaurant? = null) : RestaurantScreen()
    data class Detail(val restaurant: Restaurant) : RestaurantScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 공유 인텐트 처리
        val sharedUrl = if (intent?.action == Intent.ACTION_SEND &&
            intent.type == "text/plain"
        ) {
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        } else ""

        setContent {
            val isDarkTheme = remember { mutableStateOf(false) }
            StickyMemoTheme(darkTheme = isDarkTheme.value) {
                StickyMemoApp(
                    isDarkTheme = isDarkTheme.value,
                    onToggleDarkTheme = { isDarkTheme.value = !isDarkTheme.value },
                    initialSharedUrl = sharedUrl
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyMemoApp(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    initialSharedUrl: String = ""
) {
    val context = LocalContext.current

    // ── ViewModels ──
    val memoViewModel: MemoViewModel = viewModel(
        factory = MemoViewModelFactory(context.applicationContext as android.app.Application)
    )
    val restaurantViewModel: RestaurantViewModel = viewModel(
        factory = RestaurantViewModelFactory(context.applicationContext as android.app.Application)
    )

    // ── 탭 상태 ──
    var currentTab by remember { mutableStateOf(MainTab.MEMO) }

    // ── 메모 화면 상태 ──
    val memos by memoViewModel.allMemos.collectAsStateWithLifecycle()
    var memoScreen by remember { mutableStateOf<Screen>(Screen.List) }
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var selectedContactPhone by remember { mutableStateOf<String?>(null) }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var selectedLocationName by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }

    // ── 맛집 화면 상태 ──
    var restaurantScreen by remember { mutableStateOf<RestaurantScreen>(RestaurantScreen.List) }

    // ── 공유 URL 처리: 맛집 탭으로 이동 후 편집 화면 열기 ──
    LaunchedEffect(initialSharedUrl) {
        if (initialSharedUrl.isNotBlank()) {
            currentTab = MainTab.RESTAURANT
            restaurantScreen = RestaurantScreen.Edit(
                restaurant = Restaurant(
                    recipeUrl = initialSharedUrl,
                    recipeTitle = "공유된 레시피"
                )
            )
        }
    }

    // ── 권한 요청 ──
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isNotEmpty()) permissionLauncher.launch(denied.toTypedArray())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }
    }

    // ── 연락처 선택 다이얼로그 ──
    if (showContactPicker) {
        ContactPickerDialog(
            context = context,
            onContactSelected = { contact ->
                selectedContactName = contact.name
                selectedContactPhone = contact.phone
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }

    // ── 뒤로가기 처리 ──
    BackHandler(
        enabled = memoScreen != Screen.List ||
                restaurantScreen != RestaurantScreen.List
    ) {
        when (currentTab) {
            MainTab.MEMO -> memoScreen = Screen.List
            MainTab.RESTAURANT -> restaurantScreen = RestaurantScreen.List
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── 상단 탭 ──
        TabRow(selectedTabIndex = currentTab.ordinal) {
            Tab(
                selected = currentTab == MainTab.MEMO,
                onClick = { currentTab = MainTab.MEMO },
                text = {
                    Text(
                        "메모",
                        fontWeight = if (currentTab == MainTab.MEMO)
                            FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                },
                icon = {
                    Icon(Icons.Default.StickyNote2, contentDescription = null)
                }
            )
            Tab(
                selected = currentTab == MainTab.RESTAURANT,
                onClick = { currentTab = MainTab.RESTAURANT },
                text = {
                    Text(
                        "맛집",
                        fontWeight = if (currentTab == MainTab.RESTAURANT)
                            FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                },
                icon = {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                }
            )
        }

        // ── 탭별 화면 ──
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {

                // ── 메모 탭 ──
                MainTab.MEMO -> {
                    when (val screen = memoScreen) {
                        is Screen.List -> {
                            MemoListScreen(
                                memos = memos,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                onAddMemo = { type ->
                                    selectedContactName = null
                                    selectedContactPhone = null
                                    selectedLat = null
                                    selectedLng = null
                                    selectedLocationName = null
                                    memoScreen = Screen.Edit(type = type)
                                },
                                onMemoClick = { memo ->
                                    memoScreen = Screen.Edit(memo = memo, type = memo.type)
                                },
                                onDeleteMemo = { memo ->
                                    memoViewModel.deleteMemo(memo)
                                },
                                onTogglePin = { memo ->
                                    memoViewModel.togglePin(memo)
                                },
                                onDuplicateMemo = { memo ->
                                    memoViewModel.duplicateMemo(memo)
                                }
                            )
                        }
                        is Screen.Edit -> {
                            MemoEditScreen(
                                memo = screen.memo,
                                memoType = screen.type,
                                isDarkTheme = isDarkTheme,
                                existingTags = memos.flatMap { m ->
                                    m.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                }.distinct(),
                                onSave = { memo ->
                                    if (memo.id == 0) memoViewModel.insertMemo(memo)
                                    else memoViewModel.updateMemo(memo)
                                    if (memo.type == MemoType.LOCATION &&
                                        memo.latitude != null && memo.longitude != null
                                    ) {
                                        val helper = GeofenceHelper(context)
                                        helper.addGeofence(memo)
                                    }
                                    memoScreen = Screen.List
                                },
                                onBack = { memoScreen = Screen.List },
                                onPickContact = { showContactPicker = true },
                                onPickLocation = {
                                    getCurrentLocation(
                                        context = context,
                                        onSuccess = { lat, lng ->
                                            selectedLat = lat
                                            selectedLng = lng
                                            selectedLocationName = "현재 위치 (${
                                                String.format("%.4f", lat)}, ${
                                                String.format("%.4f", lng)})"
                                        },
                                        onFailure = { }
                                    )
                                },
                                selectedContactName = selectedContactName,
                                selectedContactPhone = selectedContactPhone,
                                selectedLat = selectedLat,
                                selectedLng = selectedLng,
                                selectedLocationName = selectedLocationName
                            )
                        }
                    }
                }

                // ── 맛집 탭 ──
                MainTab.RESTAURANT -> {
                    when (val screen = restaurantScreen) {
                        is RestaurantScreen.List -> {
                            RestaurantListScreen(
                                viewModel = restaurantViewModel,
                                onAddClick = {
                                    restaurantScreen = RestaurantScreen.Edit()
                                },
                                onItemClick = { restaurant ->
                                    restaurantScreen = RestaurantScreen.Detail(restaurant)
                                }
                            )
                        }
                        is RestaurantScreen.Edit -> {
                            val allTags by restaurantViewModel.allTags.collectAsStateWithLifecycle()
                            RestaurantEditScreen(
                                restaurant = screen.restaurant,
                                existingTags = allTags,
                                onSave = { restaurant ->
                                    if (restaurant.id == 0)
                                        restaurantViewModel.insertRestaurant(restaurant)
                                    else
                                        restaurantViewModel.updateRestaurant(restaurant)
                                    restaurantScreen = RestaurantScreen.List
                                },
                                onBack = { restaurantScreen = RestaurantScreen.List }
                            )
                        }
                        is RestaurantScreen.Detail -> {
                            RestaurantDetailScreen(
                                restaurant = screen.restaurant,
                                onBack = { restaurantScreen = RestaurantScreen.List },
                                onEdit = { restaurant ->
                                    restaurantScreen = RestaurantScreen.Edit(restaurant)
                                },
                                onDelete = { restaurant ->
                                    restaurantViewModel.deleteRestaurant(restaurant)
                                    restaurantScreen = RestaurantScreen.List
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}