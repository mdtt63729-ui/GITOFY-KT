package com.gitofy.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Home-screen widget (the web app had a WebView widget updated over the bridge).
 * Natively it is a Glance widget showing the last upload target and status.
 */
class GitofyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetState.read(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(state: WidgetState) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color(0xFF1A1112)).padding(12.dp),
        ) {
            Text("Gitofy", style = TextStyle(color = ColorProvider(Color(0xFFFFB3BA))))
            Text(
                state.target.ifBlank { "No uploads yet" },
                style = TextStyle(color = ColorProvider(Color(0xFFF0DEDF))),
                modifier = GlanceModifier.fillMaxWidth().padding(4.dp),
            )
            if (state.status.isNotBlank()) {
                Text(state.status, style = TextStyle(color = ColorProvider(Color(0xFF9F8C8D))))
            }
        }
    }
}

class GitofyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GitofyWidget()
}
