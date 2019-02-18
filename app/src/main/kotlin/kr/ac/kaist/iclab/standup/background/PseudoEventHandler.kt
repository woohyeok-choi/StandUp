package kr.ac.kaist.iclab.standup.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_PSEUDO_EXIT_FROM_STILL
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_PSEUDO_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.common.Notifications
import java.util.*
import java.util.concurrent.TimeUnit

class PseudoEventHandler private constructor() : BroadcastReceiver() {
    private val random = Random(System.currentTimeMillis())

    fun register(context: Context) {
        context.registerReceiver(this, INTENT_FILTER)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when(intent.action) {
            ACTION_PSEUDO_INTERVENTION_TRIGGER -> {
                Notifications.notifyIntervention(
                    context,
                    TimeUnit.MINUTES.toMillis(60.toLong() + random.nextInt(30)),
                    EventHandler.buildDismissAction(context),
                    EventHandler.buildSnoozeAction(context),
                    EventHandler.buildStandUpAction(context)
                )
            }

            ACTION_PSEUDO_EXIT_FROM_STILL -> {
                Notifications.notifyStandUp(
                    context,
                    TimeUnit.MINUTES.toMillis(75.toLong() + random.nextInt(30)),
                    EventHandler.buildDismissAction(context),
                    EventHandler.buildSnoozeAction(context)
                )
            }
        }
    }

    companion object {
        private val INTENT_FILTER = IntentFilter().apply {
            addAction(ACTION_PSEUDO_INTERVENTION_TRIGGER)
            addAction(ACTION_PSEUDO_EXIT_FROM_STILL)
        }

        private var instance: PseudoEventHandler? = null

        fun getInstance() : PseudoEventHandler {
            if(instance == null) {
                instance = PseudoEventHandler()
            }
            return instance!!
        }
    }
}