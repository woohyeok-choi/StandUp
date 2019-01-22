package kr.ac.kaist.iclab.standup.background

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import kr.ac.kaist.iclab.standup.common.Notifications

class SedentaryRecognitionService : Service() {
    private val eventHandler = EventHandler()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        eventHandler.handleActionEnterToStill(this, SystemClock.elapsedRealtime(), System.currentTimeMillis())

        registerReceiver(eventHandler, eventHandler.intentFilter)
        Log.d(javaClass.simpleName, "onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.notifyServiceRunning(this)
        ActivityTransitionCollector.start(this)
        Log.d(javaClass.simpleName, "onStartCommand()")

        return START_STICKY
    }

     override fun onDestroy() {
         super.onDestroy()
         eventHandler.handleActionExitFromStill(this, SystemClock.elapsedRealtime(), System.currentTimeMillis())

         unregisterReceiver(eventHandler)
         ActivityTransitionCollector.stop(this)

         Log.d(javaClass.simpleName, "onDestroy()")
     }

    companion object {
        fun newIntent(context: Context) = Intent(context, SedentaryRecognitionService::class.java)
    }
}