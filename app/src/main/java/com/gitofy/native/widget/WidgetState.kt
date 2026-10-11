package com.gitofy.native.widget

import android.content.Context

/** Tiny store the app writes and the widget reads (replaces the bridge widget API). */
data class WidgetState(val target: String, val status: String) {
    companion object {
        private const val PREFS = "gitofy_widget"

        fun read(context: Context): WidgetState {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return WidgetState(p.getString("target", "") ?: "", p.getString("status", "") ?: "")
        }

        fun write(context: Context, target: String, status: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("target", target).putString("status", status).apply()
        }
    }
}
