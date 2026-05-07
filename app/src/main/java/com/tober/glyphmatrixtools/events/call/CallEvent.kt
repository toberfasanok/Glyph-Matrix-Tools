package com.tober.glyphmatrixtools.events.call

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

import com.tober.glyphmatrixtools.events.EventService

class CallEvent : CallScreeningService() {
    private val tag = "Call Event"

    override fun onScreenCall(
        callDetails: Call.Details
    ) {
        Log.d(tag, "onScreenCall")

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)

        EventService.dispatchCallEvent(
            context = this,
            number = number
        )

        Log.d(tag, "Incoming number: $number")
    }
}
