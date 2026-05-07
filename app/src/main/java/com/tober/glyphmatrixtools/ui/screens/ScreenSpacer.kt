package com.tober.glyphmatrixtools.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScreenSpacer(
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = modifier.height(24.dp))
}
