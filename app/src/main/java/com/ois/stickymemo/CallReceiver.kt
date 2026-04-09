package com.ois.stickymemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("StickyCallReceiver", "onReceive 호출됨! action: ${intent.action}")
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        android.util.Log.d("StickyCallReceiver", "state: $state, phone: $phoneNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // 전화 수신 중
                startOverlay(context, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // 통화 연결됨 (발신 포함)
                startOverlay(context, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // 통화 종료
                stopOverlay(context)
            }
        }
    }

    private fun startOverlay(context: Context, phoneNumber: String) {
        val intent = Intent(context, CallOverlayService::class.java).apply {
            action = CallOverlayService.ACTION_START
            putExtra(CallOverlayService.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        context.startService(intent)
    }

    private fun stopOverlay(context: Context) {
        val intent = Intent(context, CallOverlayService::class.java).apply {
            action = CallOverlayService.ACTION_STOP
        }
        context.startService(intent)
    }
}