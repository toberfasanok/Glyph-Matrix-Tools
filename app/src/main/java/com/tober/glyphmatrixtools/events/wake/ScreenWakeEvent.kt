package com.tober.glyphmatrixtools.events.wake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

class ScreenWakeEvent(
    private val context: Context,
    private val listener: Listener
) {
    private val tag = "Screen Wake Event"

    interface Listener {
        fun onScreenOn()
        fun onScreenOff()
    }

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(tag, "onScreenOn")
                    listener.onScreenOn()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(tag, "onScreenOff")
                    listener.onScreenOff()
                }
            }
        }
    }

    fun start() {
        if (registered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        registered = true

        Log.d(tag, "Started")
    }

    fun stop() {
        if (!registered) return

        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w(tag, "Failed to unregister receiver: $e")
        }

        registered = false

        Log.d(tag, "Stopped")
    }
}
