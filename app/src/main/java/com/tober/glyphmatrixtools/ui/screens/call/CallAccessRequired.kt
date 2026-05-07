package com.tober.glyphmatrixtools.ui.screens.call

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
fun CallAccessRequired(
    modifier: Modifier = Modifier,

    hasCallScreeningRole: Boolean,
    hasPhoneStatePermission: Boolean,
    hasContactsPermission: Boolean,

    onOpenAppInfo: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit,
    onRequestContactsPermission: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Call Access is required",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Glyph Matrix Tools needs Call Access to detect calls."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Required steps:",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasCallScreeningRole) {
            Text(text = "Allow Glyph Matrix Tools as the Call Screening app.")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestCallScreeningRole
            ) {
                Text(text = "Allow Call Screening")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!hasPhoneStatePermission) {
            Text(text = "Allow Phone State Access so the app can detect when a call is answered or ended.")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestPhoneStatePermission
            ) {
                Text(text = "Allow Phone State Access")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!hasContactsPermission) {
            Text(text = "Allow Contacts Access so phone numbers can be matched to saved contact glyphs.")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestContactsPermission
            ) {
                Text(text = "Allow Contacts Access")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = "Allow Restricted Settings",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "If Android says any of these setting are restricted, open App Info, tap the ⋮ menu in the top-right corner, choose Allow restricted settings, then return here and continue."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenAppInfo
        ) {
            Text(text = "Open App Info")
        }
    }
}
