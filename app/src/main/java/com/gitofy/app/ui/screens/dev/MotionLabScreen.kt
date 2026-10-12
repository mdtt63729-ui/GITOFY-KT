package com.gitofy.app.ui.screens.dev

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.theme.GitofyMotion

/** Exercises the M3 motion specs so they can be tuned by eye. */
@Composable
fun MotionLabScreen(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (visible) 1f else 0.6f, animationSpec = GitofyMotion.spatialMedium)
    ScreenScaffold("Motion lab", onBack) { m ->
        Column(m.padding(16.dp), Arrangement.spacedBy(16.dp)) {
            Button(onClick = { visible = !visible }, modifier = Modifier.fillMaxWidth()) { Text("Toggle motion") }
            AnimatedVisibility(visible, enter = fadeIn() + expandVertically(animationSpec = GitofyMotion.spatialMedium)) {
                Card(Modifier.fillMaxWidth()) { Text("Animated container", Modifier.padding(16.dp)) }
            }
            Text("Scale: %.2f".format(scale), style = MaterialTheme.typography.bodySmall)
        }
    }
}
