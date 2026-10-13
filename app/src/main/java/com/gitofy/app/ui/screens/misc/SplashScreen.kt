package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gitofy.app.R

/**
 * The launch splash — just the app icon on the theme background, so it matches the
 * system's cold-start splash and reads as one continuous icon.
 *
 * There is deliberately **no spinner**: the repo cache is warmed during startup, so
 * Home opens straight into content and this never has to linger.
 */
@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.gitofy_icon),
            contentDescription = "Gitofy",
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(26.dp)),
        )
    }
}
