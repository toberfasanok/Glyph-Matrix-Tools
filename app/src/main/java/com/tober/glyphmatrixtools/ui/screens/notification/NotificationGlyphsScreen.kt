package com.tober.glyphmatrixtools.ui.screens.notification

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.tober.glyphmatrixtools.ui.fields.SwitchField
import com.tober.glyphmatrixtools.ui.glyph.DefaultGlyphItem
import com.tober.glyphmatrixtools.ui.glyph.glyphItemList
import com.tober.glyphmatrixtools.ui.glyph.rememberGlyphItemListState
import com.tober.glyphmatrixtools.ui.screens.Screen
import com.tober.glyphmatrixtools.ui.screens.ScreenSpacer
import com.tober.glyphmatrixtools.util.Constants

@Composable
fun NotificationGlyphsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val notificationAccessService = remember {
        NotificationAccessService(context)
    }

    var hasNotificationAccess by remember {
        mutableStateOf(notificationAccessService.hasAccess())
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = notificationAccessService.hasAccess()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasNotificationAccess) {
        NotificationAccessRequired(
            modifier = modifier,

            onOpenAppInfo = {
                context.startActivity(
                    notificationAccessService.createAppInfoIntent()
                )
            },
            onClickNotificationAccessSettings = {
                try {
                    context.startActivity(notificationAccessService.createSettingsIntent())
                } catch (_: Exception) {
                    context.startActivity(notificationAccessService.createFallbackSettingsIntent())
                }
            }
        )

        return
    }

    val appGlyphsState = rememberGlyphItemListState(
        title = "App Glyphs",

        preferenceKey = Constants.PREFERENCES_NOTIFICATION_APP_GLYPHS,

        useImage = true,
        imagePrefix = "notification_app_glyph",

        useApp = true,

        useContact = false
    )

    val ignoredAppsState = rememberGlyphItemListState(
        title = "Ignored Apps",

        preferenceKey = Constants.PREFERENCES_NOTIFICATION_IGNORED_APPS,

        useImage = false,

        useApp = true,

        useContact = false
    )

    val contactGlyphsState = rememberGlyphItemListState(
        title = "Contact Glyphs",

        preferenceKey = Constants.PREFERENCES_NOTIFICATION_CONTACT_GLYPHS,

        useImage = true,
        imagePrefix = "notification_contact_glyph",

        useApp = false,

        useContact = true
    )

    val ignoredContactsState = rememberGlyphItemListState(
        title = "Ignored Contacts",

        preferenceKey = Constants.PREFERENCES_NOTIFICATION_IGNORED_CONTACTS,

        useImage = false,

        useApp = false,

        useContact = true
    )

    Screen(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            SwitchField(
                title = "Active",

                preferenceKey = Constants.PREFERENCES_NOTIFICATION_GLYPHS_ACTIVE
            )
        }

        item { ScreenSpacer() }

        item {
            DefaultGlyphItem(
                title = "Default Notification Glyph",

                preferenceKey = Constants.PREFERENCES_NOTIFICATION_DEFAULT_GLYPH,

                imagePrefix = "notification_default_glyph"
            )
        }

        item { ScreenSpacer() }

        glyphItemList(
            state = appGlyphsState
        )

        item { ScreenSpacer() }

        glyphItemList(
            state = ignoredAppsState
        )

        item { ScreenSpacer() }

        glyphItemList(
            state = contactGlyphsState
        )

        item { ScreenSpacer() }

        glyphItemList(
            state = ignoredContactsState
        )
    }
}
