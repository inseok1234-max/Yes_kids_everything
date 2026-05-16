package com.ois.stickymemo.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ois.stickymemo.MainActivity

class QuickCaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        provideContent {
            QuickCaptureWidgetContent()
        }
    }
}

@Composable
private fun QuickCaptureWidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFFFF8E7)))
            .padding(12.dp)
    ) {
        Text(
            text = "StickyMemo",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Text(text = "바로 기록하기")
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Button(text = "메모", onClick = quickAction("memo"))
            Button(text = "체크", onClick = quickAction("checklist"))
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Button(text = "장소", onClick = quickAction("place"))
            Button(text = "위치", onClick = quickAction("location"))
        }
    }
}

private fun quickAction(action: String) = actionStartActivity(
    Intent().apply {
        setClassName("com.ois.stickymemo", MainActivity::class.java.name)
        putExtra(MainActivity.EXTRA_QUICK_ACTION, action)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
)

class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCaptureWidget()
}
