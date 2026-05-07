package com.tober.glyphmatrixtools.events.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import com.tober.glyphmatrixtools.events.EventService

class NotificationEvent : NotificationListenerService() {
    private val tag = "Notification Event"

    override fun onNotificationPosted(
        statusBarNotification: StatusBarNotification
    ) {
        super.onNotificationPosted(statusBarNotification)

        val packageName = statusBarNotification.packageName
        val contact = NotificationContactService.resolve(statusBarNotification.notification)

        Log.d(tag, "Notification posted from: $packageName")
        Log.d(tag, "Resolved contact: $contact")

        EventService.dispatchNotificationEvent(
            context = this,
            packageName = packageName,
            contact = contact
        )
    }

    override fun onNotificationRemoved(
        statusBarNotification: StatusBarNotification
    ) {
        super.onNotificationRemoved(statusBarNotification)

        Log.d(tag, "Notification removed from: ${statusBarNotification.packageName}")
    }
}
