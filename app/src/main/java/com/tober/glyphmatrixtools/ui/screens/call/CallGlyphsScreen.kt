package com.tober.glyphmatrixtools.ui.screens.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
fun CallGlyphsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val callAccessService = remember {
        CallAccessService(context)
    }

    var hasCallScreeningRole by remember {
        mutableStateOf(callAccessService.hasCallScreeningRole())
    }

    var hasPhoneStatePermission by remember {
        mutableStateOf(callAccessService.hasPhoneStatePermission())
    }

    var hasContactsPermission by remember {
        mutableStateOf(callAccessService.hasContactsPermission())
    }

    fun updateAccessState() {
        hasCallScreeningRole = callAccessService.hasCallScreeningRole()
        hasPhoneStatePermission = callAccessService.hasPhoneStatePermission()
        hasContactsPermission = callAccessService.hasContactsPermission()
    }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateAccessState()
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateAccessState()
    }

    val callScreeningRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateAccessState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateAccessState()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasCallScreeningRole || !hasPhoneStatePermission || !hasContactsPermission) {
        CallAccessRequired(
            modifier = modifier,

            hasCallScreeningRole = hasCallScreeningRole,
            hasPhoneStatePermission = hasPhoneStatePermission,
            hasContactsPermission = hasContactsPermission,

            onOpenAppInfo = {
                context.startActivity(
                    callAccessService.createAppInfoIntent()
                )
            },
            onRequestCallScreeningRole = {
                val intent = callAccessService.createCallScreeningRoleIntent()
                    ?: return@CallAccessRequired

                callScreeningRoleLauncher.launch(intent)
            },
            onRequestPhoneStatePermission = {
                phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            },
            onRequestContactsPermission = {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        )

        return
    }

    val contactGlyphsState = rememberGlyphItemListState(
        title = "Contact Glyphs",

        preferenceKey = Constants.PREFERENCES_CALL_CONTACT_GLYPHS,

        useImage = true,
        imagePrefix = "call_contact_glyph",

        useApp = false,

        useContact = true
    )

    val ignoredContactsState = rememberGlyphItemListState(
        title = "Ignored Contacts",

        preferenceKey = Constants.PREFERENCES_CALL_IGNORED_CONTACTS,

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

                preferenceKey = Constants.PREFERENCES_CALL_GLYPHS_ACTIVE
            )
        }

        item { ScreenSpacer() }

        item {
            DefaultGlyphItem(
                title = "Default Call Glyph",

                preferenceKey = Constants.PREFERENCES_CALL_DEFAULT_GLYPH,

                imagePrefix = "call_default_glyph"
            )
        }

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
