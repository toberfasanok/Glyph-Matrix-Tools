package com.tober.glyphmatrixtools.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.tober.glyphmatrixtools.events.EventService

class BootReceiver : BroadcastReceiver() {
    private val tag = "Boot Receiver"

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val action = intent.action

        Log.d(tag, "onReceive: $action")

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        EventService.start(context)
    }
}
