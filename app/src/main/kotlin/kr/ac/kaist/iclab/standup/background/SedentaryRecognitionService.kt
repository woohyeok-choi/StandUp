package kr.ac.kaist.iclab.standup.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kr.ac.kaist.iclab.standup.BuildConfig
import kr.ac.kaist.iclab.standup.common.MockActivityTransitionResult
import kr.ac.kaist.iclab.standup.common.Notifications

class SedentaryRecognitionService : Service() {
    private val eventHandler = EventHandler.getInstance()
    private val pseudoEventHandler = PseudoEventHandler.getInstance()

    private val activityTransitionCollector = ActivityTransitionCollector.getInstance()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass.simpleName, "onCreate()")

        wakeLock = with(getSystemService(Context.POWER_SERVICE) as PowerManager) {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.simpleName).apply {
                acquire()
            }
        }
        activityTransitionCollector.start(this)

        eventHandler.register(this)

        if(BuildConfig.DEBUG_MODE) {
            pseudoEventHandler.register(this)
        }

        sendBroadcast(MockActivityTransitionResult.still(SystemClock.elapsedRealtime()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(javaClass.simpleName, "onStartCommand()")
        Notifications.notifyServiceRunning(this, EventHandler.buildStandUpAction(this))

        return START_STICKY
    }

     override fun onDestroy() {
         Log.d(javaClass.simpleName, "onDestroy()")

         super.onDestroy()
         if(wakeLock?.isHeld == true) {
             wakeLock?.release()
         }

         activityTransitionCollector.stop(this)
         eventHandler.unregister(this)

         if(BuildConfig.DEBUG_MODE) {
             pseudoEventHandler.unregister(this)
         }
     }

    companion object {
        fun newIntent(context: Context) = Intent(context, SedentaryRecognitionService::class.java)
    }
}