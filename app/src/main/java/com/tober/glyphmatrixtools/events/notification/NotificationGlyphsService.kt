package com.tober.glyphmatrixtools.events.notification

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.glyph.GlyphStorage
import com.tober.glyphmatrixtools.util.Constants

class NotificationGlyphsService(
    context: Context
) {
    private val context = context.applicationContext
    private val packageManager = this.context.packageManager

    private val defaultGlyphStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_NOTIFICATION_DEFAULT_GLYPH
    )

    private val appGlyphsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_NOTIFICATION_APP_GLYPHS
    )

    private val ignoredAppsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_NOTIFICATION_IGNORED_APPS
    )

    private val contactGlyphsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_NOTIFICATION_CONTACT_GLYPHS
    )

    private val ignoredContactsStorage = GlyphStorage(
        context.applicationContext,
        Constants.PREFERENCES_NOTIFICATION_IGNORED_CONTACTS
    )

    fun resolveGlyph(
        packageName: String,
        contact: String
    ): Glyph? {
        val normalizedContact = contact.trim()

        val ignoredContact = ignoredContactsStorage
            .getGlyphs()
            .any { glyph ->
                normalizedContact.contains(glyph.contact.orEmpty(), ignoreCase = true)
            }

        if (ignoredContact) {
            return null
        }

        val normalizedPackageName = packageName.trim()

        if (shouldIgnorePackage(normalizedPackageName)) {
            return null
        }

        val ignoredApp = ignoredAppsStorage
            .getGlyphs()
            .any { glyph ->
                glyph.appPackageName.equals(normalizedPackageName, ignoreCase = true)
            }

        if (ignoredApp) {
            return null
        }

        val contactGlyph = contactGlyphsStorage
            .getGlyphs()
            .firstOrNull { glyph ->
                glyph.contact.equals(normalizedContact, ignoreCase = true)
            }

        if (contactGlyph != null) {
            return contactGlyph
        }

        val appGlyph = appGlyphsStorage
            .getGlyphs()
            .firstOrNull { glyph ->
                glyph.appPackageName.equals(normalizedPackageName, ignoreCase = true)
            }

        if (appGlyph != null) {
            return appGlyph
        }

        val defaultGlyph = defaultGlyphStorage.getGlyph()

        if (defaultGlyph != null) {
            return defaultGlyph
        }

        return null
    }

    private fun shouldIgnorePackage(
        packageName: String
    ): Boolean {
        if (packageName.isBlank()) return true

        if (
            packageName == "android" ||
            packageName.startsWith("android.") ||
            packageName.startsWith("com.android.")
        ) {
            return true
        }

        return try {
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )

            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            isSystemApp && !isUpdatedSystemApp
        } catch (_: Throwable) {
            false
        }
    }
}
