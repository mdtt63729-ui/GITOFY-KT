package com.gitofy.app.ui.screens.entry

import com.gitofy.app.ui.components.BouncyButton
import com.gitofy.app.ui.components.BouncyTextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.navigation.Routes
import kotlinx.coroutines.launch

private data class Slide(val title: String, val body: String, val bullets: List<String>, val icon: ImageVector)

private val SLIDES = listOf(
    Slide("Welcome to Gitofy",
        "A fast, native-feeling GitHub client built for Android — repositories, diffs, releases and Actions in one place.",
        listOf("Native Android app", "Material 3 design", "Works offline for browsing"), Icons.Filled.Android),
    Slide("Smart Diff & ZIP push",
        "Drop in a project ZIP. Gitofy extracts it, hashes every file, computes the diff against GitHub and uploads only what changed.",
        listOf("Byte-perfect extraction", "Only changed files upload", "One-tap commit & push"), Icons.Filled.FileUpload),
    Slide("Releases & APK downloads",
        "Browse every release of a repository and download its APKs and assets inside the app, with real pause and resume.",
        listOf("All release assets", "True pause / resume", "Install without leaving the app"), Icons.Filled.Download),
    Slide("Workflows & live logs",
        "Follow GitHub Actions runs as they happen — jobs, steps and the live build log, streaming inside the app.",
        listOf("Live run status", "Jobs & steps detail", "Streaming build logs"), Icons.Filled.Bolt),
    Slide("Secure by design",
        "Sign in with GitHub device flow — no secret ever ships in the app. Tokens are encrypted and an optional app lock keeps things private.",
        listOf("Secret-free device login", "Encrypted token storage", "Optional biometric lock"), Icons.Filled.Shield),
)

@Composable
fun OnboardingScreen(onNavigate: (String) -> Unit) {
    val pager = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BouncyTextButton(onClick = { onNavigate(Routes.LOGIN) }) { Text("Skip") }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val s = SLIDES[page]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(110.dp).clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(s.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text(s.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    s.body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.bullets.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(b, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(SLIDES.size) { i ->
                Box(
                    Modifier.size(if (i == pager.currentPage) 9.dp else 7.dp).clip(CircleShape)
                        .background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        BouncyButton(
            onClick = {
                if (pager.currentPage < SLIDES.size - 1) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                else onNavigate(Routes.LOGIN)
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (pager.currentPage < SLIDES.size - 1) "Next" else "Get started", fontWeight = FontWeight.Bold)
        }
    }
}
