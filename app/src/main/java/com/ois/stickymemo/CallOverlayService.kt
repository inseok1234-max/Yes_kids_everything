package com.ois.stickymemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_PHONE
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoDatabase
import com.ois.stickymemo.data.MemoType
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallOverlayService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val CHANNEL_ID = "call_overlay_channel"
        const val NOTIFICATION_ID = 2104
        private const val TAG = "StickyCallOverlay"
        private const val OVERLAY_PREFS = "call_overlay_prefs"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val DEFAULT_WIDTH_DP = 320
        private const val DEFAULT_HEIGHT_DP = 260
        private const val MIN_WIDTH_DP = 260
        private const val MIN_HEIGHT_DP = 220
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var currentPhoneNumber: String = ""
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val foregroundStarted = runCatching {
            startForeground(NOTIFICATION_ID, buildNotification())
        }.onFailure { error ->
            Log.w(TAG, "Call memo foreground service could not be started.", error)
        }.isSuccess

        if (!foregroundStarted) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START -> {
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
                showOverlay()
            }

            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission is missing. Call memo overlay cannot be shown.")
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val prefs = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density
        val defaultWidth = (DEFAULT_WIDTH_DP * density).roundToInt()
        val defaultHeight = (DEFAULT_HEIGHT_DP * density).roundToInt()
        val minWidth = (MIN_WIDTH_DP * density).roundToInt()
        val minHeight = (MIN_HEIGHT_DP * density).roundToInt()

        val params = WindowManager.LayoutParams(
            max(minWidth, prefs.getInt(KEY_WIDTH, defaultWidth)),
            max(minHeight, prefs.getInt(KEY_HEIGHT, defaultHeight)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY else TYPE_PHONE,
            FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, 16)
            y = prefs.getInt(KEY_Y, 200)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        overlayParams = params

        val owner = OverlayLifecycleOwner().also {
            it.performRestore(null)
            it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_START)
            it.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                CallMemoOverlay(
                    phoneNumber = currentPhoneNumber,
                    onMove = { deltaX, deltaY ->
                        updateOverlayBounds(
                            x = params.x + deltaX.roundToInt(),
                            y = params.y + deltaY.roundToInt(),
                            width = params.width,
                            height = params.height
                        )
                    },
                    onResize = { deltaX, deltaY ->
                        updateOverlayBounds(
                            x = params.x,
                            y = params.y,
                            width = params.width + deltaX.roundToInt(),
                            height = params.height + deltaY.roundToInt()
                        )
                    },
                    onSave = { memoText ->
                        saveMemo(memoText)
                        removeOverlay()
                        stopSelf()
                    },
                    onDismiss = {
                        removeOverlay()
                        stopSelf()
                    }
                )
            }
        }

        val frameLayout = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            addView(composeView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        runCatching {
            overlayView = frameLayout
            windowManager?.addView(overlayView, params)
        }.onFailure { error ->
            overlayView = null
            overlayParams = null
            Log.w(TAG, "Failed to add call memo overlay.", error)
            stopSelf()
        }
    }

    private fun updateOverlayBounds(x: Int, y: Int, width: Int, height: Int) {
        val params = overlayParams ?: return
        val view = overlayView ?: return
        val density = resources.displayMetrics.density
        val minWidth = (MIN_WIDTH_DP * density).roundToInt()
        val minHeight = (MIN_HEIGHT_DP * density).roundToInt()

        params.x = max(0, x)
        params.y = max(0, y)
        params.width = max(minWidth, width)
        params.height = max(minHeight, height)

        runCatching {
            windowManager?.updateViewLayout(view, params)
            saveOverlayBounds(params)
        }.onFailure { error ->
            Log.w(TAG, "Failed to update call memo overlay bounds.", error)
        }
    }

    private fun saveOverlayBounds(params: WindowManager.LayoutParams) {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_X, params.x)
            .putInt(KEY_Y, params.y)
            .putInt(KEY_WIDTH, params.width)
            .putInt(KEY_HEIGHT, params.height)
            .apply()
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
                .onFailure { Log.w(TAG, "Failed to remove call memo overlay.", it) }
            overlayView = null
            overlayParams = null
        }
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner = null
    }

    private fun getContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return runCatching {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.onFailure {
            Log.w(TAG, "Failed to resolve call contact name.", it)
        }.getOrNull()
    }

    private fun saveMemo(memoText: String) {
        val contactName = getContactName(currentPhoneNumber)
        val title = when {
            contactName != null && currentPhoneNumber.isNotBlank() -> "$contactName($currentPhoneNumber)"
            contactName != null -> contactName
            currentPhoneNumber.isNotBlank() -> currentPhoneNumber
            else -> "통화 메모"
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = MemoDatabase.getDatabase(applicationContext).memoDao()
            dao.insertMemo(
                Memo(
                    type = MemoType.CALL,
                    title = title,
                    content = memoText,
                    contactName = contactName,
                    contactPhone = currentPhoneNumber.ifBlank { null }
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StickyMemo 통화 메모",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "통화 중 메모 입력창을 표시합니다."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StickyMemo 통화 메모")
            .setContentText("통화 메모 입력창을 준비하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

@Composable
fun CallMemoOverlay(
    phoneNumber: String,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var memoText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF4D8),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onMove(dragAmount.x, dragAmount.y)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "통화 메모",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "닫기",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (phoneNumber.isNotBlank()) {
                    Text(
                        phoneNumber,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    placeholder = { Text("메모 입력") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 80.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.82f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.72f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSave(memoText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFCC00),
                        contentColor = Color(0xFF1C1C1E)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("저장")
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .background(Color(0x66000000), RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y)
                        }
                    }
            )
        }
    }
}

class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
