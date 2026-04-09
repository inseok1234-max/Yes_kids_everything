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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import com.ois.stickymemo.ui.*
import com.ois.stickymemo.ui.theme.StickyMemoTheme
import com.ois.stickymemo.viewmodel.MemoViewModel
import com.ois.stickymemo.viewmodel.MemoViewModelFactory

sealed class Screen {
    object List : Screen()
    data class Edit(val memo: Memo? = null, val type: MemoType = MemoType.NORMAL) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = remember { mutableStateOf(false) }
            StickyMemoTheme(darkTheme = isDarkTheme.value) {
                StickyMemoApp(
                    isDarkTheme = isDarkTheme.value,
                    onToggleDarkTheme = { isDarkTheme.value = !isDarkTheme.value }
                )
            }
        }
    }
}

@Composable
fun StickyMemoApp(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MemoViewModel = viewModel(
        factory = MemoViewModelFactory(context.applicationContext as android.app.Application)
    )
    val memos by viewModel.allMemos.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.List) }

    // 연락처 / 위치 선택 상태
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var selectedContactPhone by remember { mutableStateOf<String?>(null) }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var selectedLocationName by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }

    // 권한 요청
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

    when (val screen = currentScreen) {
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
                    currentScreen = Screen.Edit(type = type)
                },
                onMemoClick = { memo ->
                    currentScreen = Screen.Edit(memo = memo, type = memo.type)
                },
                onDeleteMemo = { memo ->
                    viewModel.deleteMemo(memo)
                },
                onTogglePin = { memo ->
                    viewModel.togglePin(memo)
                },
                onDuplicateMemo = { memo ->
                    viewModel.duplicateMemo(memo)
                }
            )
        }

        is Screen.Edit -> {
            BackHandler {
                currentScreen = Screen.List
            }
            MemoEditScreen(
                memo = screen.memo,
                memoType = screen.type,
                isDarkTheme = isDarkTheme,
                existingTags = memos.flatMap { m ->
                    m.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }.distinct(),
                onSave = { memo ->
                    if (memo.id == 0) viewModel.insertMemo(memo)
                    else viewModel.updateMemo(memo)

                    if (memo.type == MemoType.LOCATION &&
                        memo.latitude != null && memo.longitude != null) {
                        val helper = GeofenceHelper(context)
                        helper.addGeofence(memo)
                    }
                    currentScreen = Screen.List
                },
                onBack = { currentScreen = Screen.List },
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