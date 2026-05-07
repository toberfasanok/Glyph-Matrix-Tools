package com.tober.glyphmatrixtools.ui.screens.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NotificationAccessRequired(
    modifier: Modifier = Modifier,

    onOpenAppInfo: () -> Unit,
    onClickNotificationAccessSettings: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Notification Access is required",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Glyph Matrix Tools needs Notification Access to detect incoming notifications."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClickNotificationAccessSettings
        ) {
            Text(text = "Open Notification Access Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Allow Restricted Settings",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "If Android says this setting is restricted, open App Info, tap the ⋮ menu in the top-right corner, choose Allow restricted settings, then return here and continue."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenAppInfo
        ) {
            Text(text = "Open App Info")
        }
    }
}
