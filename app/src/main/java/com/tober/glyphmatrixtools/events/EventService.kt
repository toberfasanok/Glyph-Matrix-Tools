package com.tober.glyphmatrixtools.events

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import com.tober.glyphmatrixtools.events.call.CallContactService
import com.tober.glyphmatrixtools.events.call.CallGlyphsService
import com.tober.glyphmatrixtools.events.call.CallEventState
import com.tober.glyphmatrixtools.events.notification.NotificationGlyphsService
import com.tober.glyphmatrixtools.events.wake.ScreenWakeEvent
import com.tober.glyphmatrixtools.events.wake.ScreenWakeGlyphsService
import com.tober.glyphmatrixtools.glyph.GlyphMatrixController
import com.tober.glyphmatrixtools.util.Constants
import com.tober.glyphmatrixtools.util.PreferencesService

class EventService : Service() {
    private val tag = "Event Service"

    companion object {
        fun start(
            context: Context
        ) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, EventService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }

        private const val CHANNEL_ID = "event_service_channel"
        private const val CHANNEL_NAME = "Event Service"
        private const val NOTIFICATION_ID = 1

        fun dispatchNotificationEvent(
            context: Context,
            packageName: String,
            contact: String
        ) {
            val appContext = context.applicationContext

            val intent = Intent(appContext, EventService::class.java).apply {
                action = Constants.PREFERENCES_NOTIFICATION_ACTION_EVENT
                putExtra(Constants.PREFERENCES_NOTIFICATION_EXTRA_PACKAGE, packageName)
                putExtra(Constants.PREFERENCES_NOTIFICATION_EXTRA_CONTACT, contact)
            }

            ContextCompat.startForegroundService(appContext, intent)
        }

        fun dispatchCallEvent(
            context: Context,
            number: String
        ) {
            val appContext = context.applicationContext

            val intent = Intent(appContext, EventService::class.java).apply {
                action = Constants.PREFERENCES_CALL_ACTION_EVENT
                putExtra(Constants.PREFERENCES_CALL_EXTRA_NUMBER, number)
            }

            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    private val heartbeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val heartbeat = object : Runnable {
        override fun run() {
            Log.d(tag, "heartbeat")
            heartbeatHandler.postDelayed(this, 1000L)
        }
    }

    // Preferences
    private lateinit var preferencesService: PreferencesService

    // Glyph Matrix
    private lateinit var glyphMatrixController: GlyphMatrixController

    // Screen Wake
    private lateinit var screenWakeEvent: ScreenWakeEvent
    private lateinit var screenWakeGlyphsService: ScreenWakeGlyphsService

    // Notification
    private lateinit var notificationGlyphsService: NotificationGlyphsService

    // Call
    private lateinit var callContactService: CallContactService
    private lateinit var callGlyphsService: CallGlyphsService
    private lateinit var callEventState: CallEventState

    private data class ActiveCall(
        val number: String,
        val contact: String,
        val answered: Boolean = false
    )

    private var activeCall: ActiveCall? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        Log.d(tag, "onCreate")

        super.onCreate()

        createNotificationChannel()

        heartbeatHandler.post(heartbeat)

        // Preferences
        preferencesService = PreferencesService(applicationContext)

        // Glyph Matrix
        glyphMatrixController = GlyphMatrixController(applicationContext)
        glyphMatrixController.start()

        // Screen Wake
        screenWakeEvent = ScreenWakeEvent(
            context = applicationContext,
            listener = object : ScreenWakeEvent.Listener {
                override fun onScreenOn() {
                    Log.d(tag, "onScreenOn")
                    onScreenOnEvent()
                }

                override fun onScreenOff() {
                    Log.d(tag, "onScreenOff")
                    onScreenOffEvent()
                }
            }
        )
        screenWakeEvent.start()

        screenWakeGlyphsService = ScreenWakeGlyphsService(applicationContext)

        // Notification
        notificationGlyphsService = NotificationGlyphsService(applicationContext)

        // Call
        callContactService = CallContactService(applicationContext)
        callGlyphsService = CallGlyphsService(applicationContext)

        callEventState = CallEventState(
            context = applicationContext,
            listener = object : CallEventState.Listener {
                override fun onCallRinging() {
                    Log.d(tag, "onCallRinging")
                }

                override fun onCallAnswered() {
                    Log.d(tag, "onCallAnswered")
                    onCallAnsweredEvent()
                }

                override fun onCallIdle() {
                    Log.d(tag, "onCallIdle")
                    onCallEndedEvent()
                }
            }
        )

        callEventState.start()
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy")

        heartbeatHandler.removeCallbacks(heartbeat)

        // Glyph Matrix
        glyphMatrixController.stop()

        // Screen Wake
        screenWakeEvent.stop()

        // Call
        callEventState.stop()
        serviceScope.cancel()

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(tag, "onStartCommand")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Glyph Matrix Tools")
            .setContentText("Glyph Matrix Tools are active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        // Notification
        if (intent?.action == Constants.PREFERENCES_NOTIFICATION_ACTION_EVENT) {
            onNotificationEvent(intent)
        }

        // Call
        callEventState.start()

        if (intent?.action == Constants.PREFERENCES_CALL_ACTION_EVENT) {
            onCallEvent(intent)
        }

        return START_STICKY
    }

    private fun onScreenOnEvent() {
        onScreenWakeEvent()
    }

    private fun onScreenOffEvent() {
        if (activeCall != null) {
            Log.d(tag, "Ignoring onScreenOffEvent while call is active")
            return
        }

        glyphMatrixController.clear()
    }

    // Screen Wake
    private fun onScreenWakeEvent() {
        val active = preferencesService.getBoolean(
            Constants.PREFERENCES_SCREEN_WAKE_GLYPHS_ACTIVE,
            true
        )

        if (!active) {
            Log.d(tag, "Screen Wake Glyphs inactive")
            return
        }

        val glyph = screenWakeGlyphsService.resolveGlyph()

        if (glyph == null) {
            Log.d(tag, "No Screen Wake Glyph resolved")
            return
        }

        val timeout = preferencesService
            .getLong(Constants.PREFERENCES_SCREEN_WAKE_GLYPH_TIMEOUT, 5L)
            .coerceAtLeast(1L) * 1000L

        glyphMatrixController.show(
            glyph,
            timeout
        )
    }

    // Notification
    private fun onNotificationEvent(
        intent: Intent
    ) {
        val active = preferencesService.getBoolean(
            Constants.PREFERENCES_NOTIFICATION_GLYPHS_ACTIVE,
            true
        )

        if (!active) {
            Log.d(tag, "Notification Glyphs inactive")
            return
        }

        val packageName = intent.getStringExtra(Constants.PREFERENCES_NOTIFICATION_EXTRA_PACKAGE).orEmpty()
        val contact = intent.getStringExtra(Constants.PREFERENCES_NOTIFICATION_EXTRA_CONTACT).orEmpty()

        val glyph = notificationGlyphsService.resolveGlyph(
            packageName,
            contact
        )

        if (glyph == null) {
            Log.d(tag, "No Notification Glyph resolved")
            return
        }

        val timeout = preferencesService
            .getLong(Constants.PREFERENCES_NOTIFICATION_GLYPH_TIMEOUT, 5L)
            .coerceAtLeast(1L) * 1000L

        glyphMatrixController.show(
            glyph,
            timeout
        )
    }

    // Call
    private fun onCallEvent(
        intent: Intent
    ) {
        val active = preferencesService.getBoolean(
            Constants.PREFERENCES_CALL_GLYPHS_ACTIVE,
            true
        )

        if (!active) {
            Log.d(tag, "Call Glyphs inactive")
            return
        }

        val number = intent.getStringExtra(Constants.PREFERENCES_CALL_EXTRA_NUMBER).orEmpty()

        serviceScope.launch {
            val contact = callContactService.resolve(number)

            Log.d(tag, "Resolved call contact: $contact")

            activeCall = ActiveCall(
                number = number,
                contact = contact
            )

            val glyph = callGlyphsService.resolveGlyph(
                number = number,
                contact = contact
            )

            if (glyph == null) {
                Log.d(tag, "No Call Glyph resolved")
                return@launch
            }

            glyphMatrixController.showPersistent(glyph)
        }
    }

    private fun onCallAnsweredEvent() {
        val call = activeCall ?: return

        activeCall = call.copy(
            answered = true
        )
    }

    private fun onCallEndedEvent() {
        val call = activeCall ?: return

        if (call.answered) {
            Log.d(tag, "Call ended after answer")
        } else {
            Log.d(tag, "Call ended before answer")
        }

        activeCall = null

        glyphMatrixController.clear()
    }
}
