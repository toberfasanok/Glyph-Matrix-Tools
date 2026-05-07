package com.tober.glyphmatrixtools.util

import android.content.Context
import androidx.core.content.edit

class PreferencesService(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        Constants.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getBoolean(
        key: String,
        defaultValue: Boolean = true
    ): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun setBoolean(
        key: String,
        value: Boolean
    ) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    fun getString(
        key: String,
        defaultValue: String? = null
    ): String? {
        return preferences.getString(key, defaultValue)
    }

    fun setString(
        key: String,
        value: String?
    ) {
        preferences.edit {
            putString(key, value)
        }
    }

    fun getLong(
        key: String,
        defaultValue: Long = 0L
    ): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun setLong(
        key: String,
        value: Long
    ) {
        preferences.edit {
            putLong(key, value)
        }
    }

    fun remove(
        key: String
    ) {
        preferences.edit {
            remove(key)
        }
    }
}
