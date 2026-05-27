package com.ois.stickymemo.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ois.stickymemo.MainActivity
import com.ois.stickymemo.R

class QuickCaptureWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(160.dp, 72.dp))
    )

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent {
            QuickMemoWidgetContent()
        }
    }
}

@Composable
private fun QuickMemoWidgetContent() {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetYellow))
            .clickable(openMemoEditorAction())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .background(ColorProvider(Color(0xEFFFFFFF))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_note_logo),
                contentDescription = "StickyMemo"
            )
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Text(
            text = "메모 작성",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF1C1C1E))
            )
        )
    }
}

private val WidgetYellow = Color(0xFFFFCC00)

private fun openMemoEditorAction() = actionStartActivity(
    Intent().apply {
        setClassName("com.ois.stickymemo", MainActivity::class.java.name)
        putExtra(MainActivity.EXTRA_QUICK_ACTION, "memo")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
)

class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCaptureWidget()
}
