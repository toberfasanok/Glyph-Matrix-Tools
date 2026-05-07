package com.tober.glyphmatrixtools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.tober.glyphmatrixtools.events.EventService
import com.tober.glyphmatrixtools.ui.Ui
import com.tober.glyphmatrixtools.ui.theme.GlyphMatrixToolsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        EventService.start(this)

        setContent {
            GlyphMatrixToolsTheme {
                Ui()
            }
        }
    }
}
