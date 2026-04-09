package com.ois.stickymemo

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.app.Service
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoDatabase
import com.ois.stickymemo.data.MemoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallOverlayService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val CHANNEL_ID = "call_overlay_channel"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentPhoneNumber: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                showOverlay()
            }
            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun getContactName(phoneNumber: String): String? {
        if (phoneNumber.isEmpty()) return null
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(phoneNumber)
            .build()
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                TYPE_APPLICATION_OVERLAY
            else
                TYPE_PHONE,
            FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                CallMemoOverlay(
                    phoneNumber = currentPhoneNumber,
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

        val frameLayout = FrameLayout(this)
        frameLayout.addView(composeView)

        overlayView = frameLayout
        windowManager?.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    private fun saveMemo(memoText: String) {
        val contactName = getContactName(currentPhoneNumber)
        val title = if (contactName != null) {
            "$contactName($currentPhoneNumber)"
        } else {
            currentPhoneNumber.ifEmpty { "통화 메모" }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = MemoDatabase.getDatabase(applicationContext).memoDao()
            dao.insertMemo(
                Memo(
                    type = MemoType.CALL,
                    title = title,
                    content = memoText,
                    contactName = contactName,
                    contactPhone = currentPhoneNumber
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}

@Composable
fun CallMemoOverlay(
    phoneNumber: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var memoText by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF176),
        shadowElevation = 8.dp,
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📞 통화 메모",
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
            if (phoneNumber.isNotEmpty()) {
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
                placeholder = { Text("메모 입력...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSave(memoText) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF9A825)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("저장")
            }
        }
    }
}

class MyLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
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