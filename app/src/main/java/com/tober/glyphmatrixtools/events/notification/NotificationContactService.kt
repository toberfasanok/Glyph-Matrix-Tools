package com.tober.glyphmatrixtools.events.notification

import android.app.Notification
import android.app.Person
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat

object NotificationContactService {
    private const val TAG = "Notification Contact Service"

    fun resolve(
        notification: Notification
    ): String {
        val extras = notification.extras

        try {
            val people: ArrayList<Person>? = extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, Person::class.java)

            if (!people.isNullOrEmpty()) {
                val person = people[0]
                person.name?.toString()?.takeIf { it.isNotBlank() }?.let {
                    return it
                }

                return person.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "People extraction failed: $e")
        }

        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

            if (messagingStyle != null) {
                messagingStyle.conversationTitle
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        return it
                    }

                val messages = messagingStyle.messages

                if (!messages.isNullOrEmpty()) {
                    val last = messages.last()

                    try {
                        val getSender = last.javaClass.getMethod("getSender")
                        val sender = getSender.invoke(last) as? CharSequence

                        if (!sender.isNullOrBlank()) {
                            return sender.toString()
                        }
                    } catch (_: Exception) {
                    }

                    try {
                        val getBundle = last.javaClass.getMethod("getData")
                        val data = getBundle.invoke(last)

                        if (data is Bundle) {
                            val sender = data.getString("sender")
                                ?: data.getCharSequence("sender")?.toString()

                            if (!sender.isNullOrBlank()) {
                                return sender
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MessagingStyle extraction failed: $e")
        }

        try {
            val parcels = extras.getParcelableArray("android.messages", Bundle::class.java)

            if (parcels != null) {
                for (parcel in parcels) {
                    val sender = parcel.getString("sender")
                        ?: parcel.getCharSequence("sender")?.toString()

                    if (!sender.isNullOrBlank()) {
                        return sender
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android messages extraction failed: $e")
        }

        try {
            val title = extras.getCharSequence("android.conversationTitle")?.toString() ?: extras.getCharSequence("android.title")?.toString()

            if (!title.isNullOrBlank()) {
                return title
            }
        } catch (_: Exception) {
        }

        try {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

            if (!title.isNullOrBlank()) {
                return title
            }
        } catch (_: Exception) {
        }

        return ""
    }
}
