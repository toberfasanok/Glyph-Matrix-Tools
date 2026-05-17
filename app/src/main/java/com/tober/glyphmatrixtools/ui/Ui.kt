package com.tober.glyphmatrixtools.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.tober.glyphmatrixtools.ui.modifiers.clearFocusOnTap
import com.tober.glyphmatrixtools.ui.navigation.NavigationItem
import com.tober.glyphmatrixtools.ui.navigation.Navigation
import com.tober.glyphmatrixtools.ui.screens.call.CallGlyphsScreen
import com.tober.glyphmatrixtools.ui.screens.canvas.GlyphCanvasScreen
import com.tober.glyphmatrixtools.ui.screens.notification.NotificationGlyphsScreen
import com.tober.glyphmatrixtools.ui.screens.settings.SettingsScreen
import com.tober.glyphmatrixtools.ui.screens.wake.ScreenWakeGlyphsScreen

typealias AppTopBarActions = @Composable RowScope.() -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ui() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedDestination by remember {
        mutableStateOf(NavigationItem.GlyphCanvas)
    }

    val emptyTopBarActions: AppTopBarActions = {}

    var topBarActions by remember {
        mutableStateOf(emptyTopBarActions)
    }

    LaunchedEffect(selectedDestination) {
        if (selectedDestination != NavigationItem.GlyphCanvas) {
            topBarActions = emptyTopBarActions
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            Navigation(
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        },
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            modifier = Modifier.clearFocusOnTap(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = selectedDestination.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open navigation menu",
                                modifier = Modifier.padding(horizontal = 6.dp)
                                    .size(30.dp)
                            )
                        }
                    },
                    actions = topBarActions
                )
            }
        ) { innerPadding ->
            when (selectedDestination) {
                NavigationItem.GlyphCanvas -> {
                    GlyphCanvasScreen(
                        modifier = Modifier.padding(innerPadding),
                        onTopBarActionsChange = { actions ->
                            topBarActions = actions
                        }
                    )
                }

                NavigationItem.ScreenWakeGlyphs -> {
                    ScreenWakeGlyphsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavigationItem.NotificationGlyphs -> {
                    NotificationGlyphsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavigationItem.CallGlyphs -> {
                    CallGlyphsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavigationItem.Settings -> {
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
