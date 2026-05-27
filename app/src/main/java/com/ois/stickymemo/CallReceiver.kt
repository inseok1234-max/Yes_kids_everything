package com.ois.stickymemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StickyCallReceiver", "onReceive action=${intent.action}")
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        Log.d("StickyCallReceiver", "state=$state, hasPhoneNumber=${phoneNumber.isNotBlank()}")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> startOverlay(context, phoneNumber)

            TelephonyManager.EXTRA_STATE_IDLE -> stopOverlay(context)
        }
    }

    private fun startOverlay(context: Context, phoneNumber: String) {
        val intent = Intent(context, CallOverlayService::class.java).apply {
            action = CallOverlayService.ACTION_START
            putExtra(CallOverlayService.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        startCallOverlayService(context, intent)
    }

    private fun stopOverlay(context: Context) {
        val intent = Intent(context, CallOverlayService::class.java).apply {
            action = CallOverlayService.ACTION_STOP
        }
        startCallOverlayService(context, intent)
    }

    private fun startCallOverlayService(context: Context, intent: Intent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onFailure { error ->
            Log.w(
                "StickyCallReceiver",
                "Call memo service could not be started from phone-state broadcast.",
                error
            )
        }
    }
}
