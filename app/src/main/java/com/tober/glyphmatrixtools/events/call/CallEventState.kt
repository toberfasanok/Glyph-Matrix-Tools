package com.tober.glyphmatrixtools.events.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallEventState(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onCallRinging()
        fun onCallAnswered()
        fun onCallIdle()
    }

    private val tag = "Call Event State"

    private val context = context.applicationContext
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    private var registered = false

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(
            state: Int
        ) {
            Log.d(tag, "Call state changed: $state")

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d(tag, "onCallRinging")
                    listener.onCallRinging()
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d(tag, "onCallAnswered")
                    listener.onCallAnswered()
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d(tag, "onCallIdle")
                    listener.onCallIdle()
                }
            }
        }
    }

    fun start() {
        if (registered) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(tag, "READ_PHONE_STATE not granted")
            return
        }

        try {
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )

            registered = true

            Log.d(tag, "Started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start: $e")
        }
    }

    fun stop() {
        if (!registered) return

        try {
            telephonyManager.unregisterTelephonyCallback(callback)
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop: $e")
        }

        registered = false

        Log.d(tag, "Stopped")
    }
}
