package com.gitofy.app.ui.screens.entry

import com.gitofy.app.ui.components.BouncyButton
import com.gitofy.app.ui.components.BouncyOutlinedButton
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gitofy.app.permissions.PermissionHelper
import com.gitofy.app.ui.components.ScreenScaffold

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(PermissionHelper.hasNotifications(context)) }
    var install by remember { mutableStateOf(PermissionHelper.canInstallPackages(context)) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted
    }

    ScreenScaffold("Permissions", onBack) { m ->
        Column(m.padding(20.dp), Arrangement.spacedBy(12.dp)) {
            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            Text("For workflow run and upload updates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (Build.VERSION.SDK_INT >= 33 && !notifications) {
                BouncyButton(onClick = { notifLauncher.launch(PermissionHelper.notificationPermission()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow notifications")
                }
            } else Text("Granted ✓")

            Text("Install unknown apps", style = MaterialTheme.typography.titleMedium)
            Text("Only used by the in-app updater.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!install) {
                BouncyOutlinedButton(onClick = { context.startActivity(PermissionHelper.installPackagesIntent(context)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open settings")
                }
            } else Text("Granted ✓")

            Text("Internet", style = MaterialTheme.typography.titleMedium)
            Text("Required to talk to GitHub.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
