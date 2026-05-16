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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import com.ois.stickymemo.data.Restaurant
import com.ois.stickymemo.ui.ContactPickerDialog
import com.ois.stickymemo.ui.GeofenceHelper
import com.ois.stickymemo.ui.HomeScreen
import com.ois.stickymemo.ui.MemoEditScreen
import com.ois.stickymemo.ui.RecordSource
import com.ois.stickymemo.ui.RecordsScreen
import com.ois.stickymemo.ui.RestaurantDetailScreen
import com.ois.stickymemo.ui.RestaurantEditScreen
import com.ois.stickymemo.ui.RestaurantListScreen
import com.ois.stickymemo.ui.SettingsScreen
import com.ois.stickymemo.ui.StickyRadius
import com.ois.stickymemo.ui.getCurrentLocation
import com.ois.stickymemo.ui.theme.StickyMemoTheme
import com.ois.stickymemo.viewmodel.MemoViewModel
import com.ois.stickymemo.viewmodel.MemoViewModelFactory
import com.ois.stickymemo.viewmodel.RestaurantViewModel
import com.ois.stickymemo.viewmodel.RestaurantViewModelFactory

enum class MainDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("홈", Icons.Default.Home),
    RECORDS("기록", Icons.Default.StickyNote2),
    PLACES("장소", Icons.Default.Place),
    SETTINGS("설정", Icons.Default.Settings)
}

sealed class Screen {
    object List : Screen()
    data class Edit(val memo: Memo? = null, val type: MemoType = MemoType.NORMAL) : Screen()
}

sealed class RestaurantScreen {
    object List : RestaurantScreen()
    data class Edit(val restaurant: Restaurant? = null) : RestaurantScreen()
    data class Detail(val restaurant: Restaurant) : RestaurantScreen()
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_QUICK_ACTION = "com.ois.stickymemo.EXTRA_QUICK_ACTION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        } else {
            ""
        }
        val quickAction = intent?.getStringExtra(EXTRA_QUICK_ACTION).orEmpty()

        setContent {
            val isDarkTheme = remember { mutableStateOf(false) }
            StickyMemoTheme(darkTheme = isDarkTheme.value) {
                StickyMemoApp(
                    isDarkTheme = isDarkTheme.value,
                    onToggleDarkTheme = { isDarkTheme.value = !isDarkTheme.value },
                    initialSharedUrl = sharedUrl,
                    initialQuickAction = quickAction
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
    initialSharedUrl: String = "",
    initialQuickAction: String = ""
) {
    val context = LocalContext.current
    val memoViewModel: MemoViewModel = viewModel(
        factory = MemoViewModelFactory(context.applicationContext as android.app.Application)
    )
    val restaurantViewModel: RestaurantViewModel = viewModel(
        factory = RestaurantViewModelFactory(context.applicationContext as android.app.Application)
    )

    val memos by memoViewModel.allMemos.collectAsStateWithLifecycle()
    val restaurants by restaurantViewModel.restaurants.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var memoScreen by remember { mutableStateOf<Screen>(Screen.List) }
    var restaurantScreen by remember { mutableStateOf<RestaurantScreen>(RestaurantScreen.List) }
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var selectedContactPhone by remember { mutableStateOf<String?>(null) }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var selectedLocationName by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }

    val currentLocationFormat = stringResource(R.string.current_location_format)
    val sharedRecipeTitle = stringResource(R.string.shared_recipe_title)

    fun resetMemoDraftContext() {
        selectedContactName = null
        selectedContactPhone = null
        selectedLat = null
        selectedLng = null
        selectedLocationName = null
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showContactPicker = true
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getCurrentLocation(
                context = context,
                onSuccess = { lat, lng ->
                    selectedLat = lat
                    selectedLng = lng
                    selectedLocationName = currentLocationFormat.format(
                        String.format("%.4f", lat),
                        String.format("%.4f", lng)
                    )
                },
                onFailure = { }
            )
        }
    }
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestLocationForMemo() {
        requestNotificationPermissionIfNeeded()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation(
                context = context,
                onSuccess = { lat, lng ->
                    selectedLat = lat
                    selectedLng = lng
                    selectedLocationName = currentLocationFormat.format(
                        String.format("%.4f", lat),
                        String.format("%.4f", lng)
                    )
                },
                onFailure = { }
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun requestBackgroundLocationOptInIfNeeded() {
        requestNotificationPermissionIfNeeded()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            requestLocationForMemo()
        }
    }

    fun requestCallMemoOptInIfNeeded() {
        val deniedPermissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (deniedPermissions.isNotEmpty()) {
            callPermissionLauncher.launch(deniedPermissions.toTypedArray())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(context)
        ) {
            overlaySettingsLauncher.launch(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    fun openMemoEditor(type: MemoType) {
        resetMemoDraftContext()
        if (type == MemoType.CALL) requestCallMemoOptInIfNeeded()
        if (type == MemoType.LOCATION) requestLocationForMemo()
        currentDestination = MainDestination.RECORDS
        memoScreen = Screen.Edit(type = type)
    }

    fun openPlaceEditor(restaurant: Restaurant? = null) {
        currentDestination = MainDestination.PLACES
        restaurantScreen = RestaurantScreen.Edit(restaurant)
    }

    LaunchedEffect(initialSharedUrl) {
        if (initialSharedUrl.isNotBlank()) {
            openPlaceEditor(
                Restaurant(
                    recipeUrl = initialSharedUrl,
                    recipeTitle = sharedRecipeTitle
                )
            )
        }
    }

    LaunchedEffect(initialQuickAction) {
        when (initialQuickAction) {
            "memo" -> openMemoEditor(MemoType.NORMAL)
            "checklist" -> openMemoEditor(MemoType.CHECKLIST)
            "place" -> openPlaceEditor()
            "location" -> openMemoEditor(MemoType.LOCATION)
        }
    }

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

    if (showCaptureSheet) {
        QuickCaptureSheetPolished(
            onDismiss = { showCaptureSheet = false },
            onMemo = {
                showCaptureSheet = false
                openMemoEditor(MemoType.NORMAL)
            },
            onChecklist = {
                showCaptureSheet = false
                openMemoEditor(MemoType.CHECKLIST)
            },
            onPlace = {
                showCaptureSheet = false
                openPlaceEditor()
            },
            onLocation = {
                showCaptureSheet = false
                openMemoEditor(MemoType.LOCATION)
            },
            onCall = {
                showCaptureSheet = false
                openMemoEditor(MemoType.CALL)
            }
        )
    }

    val isInNestedScreen = memoScreen != Screen.List || restaurantScreen != RestaurantScreen.List
    BackHandler(enabled = isInNestedScreen) {
        memoScreen = Screen.List
        restaurantScreen = RestaurantScreen.List
    }

    Scaffold(
        bottomBar = {
            if (!isInNestedScreen) {
                NavigationBar {
                    MainDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isInNestedScreen) {
                val fabScale by animateFloatAsState(
                    targetValue = if (showCaptureSheet) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "global_fab_scale"
                )
                FloatingActionButton(
                    onClick = { showCaptureSheet = true },
                    modifier = Modifier.graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "새 기록")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentDestination) {
                MainDestination.HOME -> HomeScreen(
                    memos = memos,
                    places = restaurants,
                    onQuickMemo = { openMemoEditor(MemoType.NORMAL) },
                    onMemoClick = { memo ->
                        currentDestination = MainDestination.RECORDS
                        memoScreen = Screen.Edit(memo = memo, type = memo.type)
                    },
                    onPlaceClick = { place ->
                        currentDestination = MainDestination.PLACES
                        restaurantScreen = RestaurantScreen.Detail(place)
                    }
                )

                MainDestination.RECORDS -> when (val screen = memoScreen) {
                    is Screen.List -> RecordsScreen(
                        memos = memos,
                        places = restaurants,
                        onRecordClick = { record ->
                            when (val source = record.source) {
                                is RecordSource.MemoRecord -> {
                                    memoScreen = Screen.Edit(
                                        memo = source.memo,
                                        type = source.memo.type
                                    )
                                }
                                is RecordSource.PlaceRecord -> {
                                    currentDestination = MainDestination.PLACES
                                    restaurantScreen = RestaurantScreen.Detail(source.place)
                                }
                            }
                        }
                    )

                    is Screen.Edit -> MemoEditRoute(
                        screen = screen,
                        memos = memos,
                        isDarkTheme = isDarkTheme,
                        selectedContactName = selectedContactName,
                        selectedContactPhone = selectedContactPhone,
                        selectedLat = selectedLat,
                        selectedLng = selectedLng,
                        selectedLocationName = selectedLocationName,
                        onSave = { memo ->
                            if (memo.id == 0) memoViewModel.insertMemo(memo) else memoViewModel.updateMemo(memo)
                            if (memo.type == MemoType.LOCATION && memo.latitude != null && memo.longitude != null) {
                                GeofenceHelper(context).addGeofence(memo)
                            }
                            memoScreen = Screen.List
                        },
                        onBack = { memoScreen = Screen.List },
                        onPickContact = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                showContactPicker = true
                            } else {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        onPickLocation = { requestLocationForMemo() }
                    )
                }

                MainDestination.PLACES -> when (val screen = restaurantScreen) {
                    is RestaurantScreen.List -> RestaurantListScreen(
                        viewModel = restaurantViewModel,
                        showFab = false,
                        onAddClick = { openPlaceEditor() },
                        onItemClick = { restaurant -> restaurantScreen = RestaurantScreen.Detail(restaurant) }
                    )

                    is RestaurantScreen.Edit -> {
                        val allTags by restaurantViewModel.allTags.collectAsStateWithLifecycle()
                        RestaurantEditScreen(
                            restaurant = screen.restaurant,
                            existingTags = allTags,
                            onSave = { restaurant ->
                                if (restaurant.id == 0) {
                                    restaurantViewModel.insertRestaurant(restaurant)
                                } else {
                                    restaurantViewModel.updateRestaurant(restaurant)
                                }
                                restaurantScreen = RestaurantScreen.List
                            },
                            onBack = { restaurantScreen = RestaurantScreen.List }
                        )
                    }

                    is RestaurantScreen.Detail -> RestaurantDetailScreen(
                        restaurant = screen.restaurant,
                        onBack = { restaurantScreen = RestaurantScreen.List },
                        onEdit = { restaurant -> restaurantScreen = RestaurantScreen.Edit(restaurant) },
                        onDelete = { restaurant ->
                            restaurantViewModel.deleteRestaurant(restaurant)
                            restaurantScreen = RestaurantScreen.List
                        }
                    )
                }

                MainDestination.SETTINGS -> SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onEnableCallMemo = { requestCallMemoOptInIfNeeded() },
                    onEnableBackgroundLocation = { requestBackgroundLocationOptInIfNeeded() }
                )
            }
        }
    }
}

@Composable
private fun MemoEditRoute(
    screen: Screen.Edit,
    memos: List<Memo>,
    isDarkTheme: Boolean,
    selectedContactName: String?,
    selectedContactPhone: String?,
    selectedLat: Double?,
    selectedLng: Double?,
    selectedLocationName: String?,
    onSave: (Memo) -> Unit,
    onBack: () -> Unit,
    onPickContact: () -> Unit,
    onPickLocation: () -> Unit
) {
    MemoEditScreen(
        memo = screen.memo,
        memoType = screen.type,
        isDarkTheme = isDarkTheme,
        existingTags = memos.flatMap { memo ->
            memo.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct(),
        onSave = onSave,
        onBack = onBack,
        onPickContact = onPickContact,
        onPickLocation = onPickLocation,
        selectedContactName = selectedContactName,
        selectedContactPhone = selectedContactPhone,
        selectedLat = selectedLat,
        selectedLng = selectedLng,
        selectedLocationName = selectedLocationName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureSheetPolished(
    onDismiss: () -> Unit,
    onMemo: () -> Unit,
    onChecklist: () -> Unit,
    onPlace: () -> Unit,
    onLocation: () -> Unit,
    onCall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = StickyRadius.sheet,
            topEnd = StickyRadius.sheet
        ),
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("빠른 메모") },
                supportingContent = { Text("떠오른 내용을 바로 붙잡습니다") },
                leadingContent = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onMemo)
            )
            ListItem(
                headlineContent = { Text("체크리스트") },
                supportingContent = { Text("해야 할 일을 항목으로 기록합니다") },
                leadingContent = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onChecklist)
            )
            ListItem(
                headlineContent = { Text("장소 기록") },
                supportingContent = { Text("카페, 병원, 여행지, 거래처를 함께 기록합니다") },
                leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onPlace)
            )
            ListItem(
                headlineContent = { Text("위치 알림") },
                supportingContent = { Text("장소에 도착했을 때 다시 볼 메모입니다") },
                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onLocation)
            )
            ListItem(
                headlineContent = { Text("통화 메모") },
                supportingContent = { Text("고급 기능 권한 확인 후 사용할 수 있습니다") },
                leadingContent = { Icon(Icons.Default.Call, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onCall)
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Text("닫기", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    onMemo: () -> Unit,
    onChecklist: () -> Unit,
    onPlace: () -> Unit,
    onLocation: () -> Unit,
    onCall: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("빠른 메모") },
                supportingContent = { Text("떠오른 내용을 바로 붙잡습니다") },
                leadingContent = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onMemo)
            )
            ListItem(
                headlineContent = { Text("체크리스트") },
                supportingContent = { Text("해야 할 일을 항목으로 기록합니다") },
                leadingContent = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onChecklist)
            )
            ListItem(
                headlineContent = { Text("장소 기록") },
                supportingContent = { Text("맛집, 카페, 병원, 거래처를 함께 기록합니다") },
                leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onPlace)
            )
            ListItem(
                headlineContent = { Text("위치 알림") },
                supportingContent = { Text("장소에 도착했을 때 다시 볼 메모입니다") },
                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onLocation)
            )
            ListItem(
                headlineContent = { Text("통화 메모") },
                supportingContent = { Text("고급 기능 권한 확인 후 사용할 수 있습니다") },
                leadingContent = { Icon(Icons.Default.Call, contentDescription = null) },
                modifier = Modifier.clickableNoIndication(onCall)
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Text("닫기", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = this.then(
    Modifier.padding(horizontal = 8.dp)
).then(
    Modifier.clickable(onClick = onClick)
)
