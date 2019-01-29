package kr.ac.kaist.iclab.standup.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_INTERVENTION_DISMISS
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_INTERVENTION_SNOOZED
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_MOCK_EXIT_FROM_STILL
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_TRANSITION_UPDATE
import kr.ac.kaist.iclab.standup.common.DayOfWeek
import kr.ac.kaist.iclab.standup.common.LocalTime
import kr.ac.kaist.iclab.standup.common.Notifications.dismissIntervention
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_INTERVENTION_SNOOZED
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.common.ConfigManager
import kr.ac.kaist.iclab.standup.common.DateTimes.elapsedTimeToLocalTime
import kr.ac.kaist.iclab.standup.common.Notifications
import kr.ac.kaist.iclab.standup.common.Notifications.notifyIntervention
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_INTERVENTION_DISMISS
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_MOCK_EXIT_FROM_STILL
import kr.ac.kaist.iclab.standup.entity.*
import java.util.concurrent.TimeUnit

class EventHandler private constructor(): BroadcastReceiver() {
    val status = Status.newLiveData()

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(javaClass.simpleName, "onReceive(intent.action: ${intent?.action})")

        if (context == null || intent == null) return

        when(intent.action) {
            ACTION_TRANSITION_UPDATE -> if(ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult.extractResult(intent)?.transitionEvents?.filter {
                    it.activityType == DetectedActivity.STILL
                }?.sortedBy {
                    it.elapsedRealTimeNanos
                }?.forEach {
                    val elapsedTime = TimeUnit.NANOSECONDS.toMillis(it.elapsedRealTimeNanos)

                    if(it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        handleActionEnterToStill(context, elapsedTime)
                    } else {
                        handleActionExitFromStill(context, elapsedTime)
                    }
                }
            }
            ACTION_INTERVENTION_TRIGGER -> if(intent.hasExtra(EXTRA_SEDENTARY_START_TIME)) {
                handleActionInterventionTrigger(context, intent.getLongExtra(EXTRA_SEDENTARY_START_TIME, 0L))
            }
            ACTION_MOCK_EXIT_FROM_STILL -> handleMockExitFromStill(context)
            ACTION_INTERVENTION_DISMISS -> handleActionDismiss(context)
            ACTION_INTERVENTION_SNOOZED -> handleActionSnooze(context)
        }
    }

    fun register(context: Context) {
        context.registerReceiver(this, INTENT_FILTER)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    fun handleActionEnterToStill(context: Context, elapsedTime: Long) {
        val tag = javaClass.simpleName

        val latestSedentaryEvent = with(App.boxStore.boxFor<PhysicalActivity>()) {
            val latestEvent = PhysicalActivity.latestActivity(this)
            Log.d(tag, "handleActionEnterToStill(elapsedTime: $elapsedTime) : latestEvent = $latestEvent")

            if(latestEvent?.eventType != PhysicalActivity.TYPE_SEDENTARY) {
                PhysicalActivity.sedentary(this, elapsedTime, latestEvent)
            } else {
                latestEvent
            }
        }

        val configManager = ConfigManager.getInstance(context)
        val interval = TimeUnit.MINUTES.toMillis(configManager.interventionInitIntervalMin.toLong())
        val triggerAt = latestSedentaryEvent.startElapsedTimeMillis + interval

        scheduleIntervention(context, triggerAt, latestSedentaryEvent.startElapsedTimeMillis)

        val localTime = elapsedTimeToLocalTime(latestSedentaryEvent.startElapsedTimeMillis)
        val triggerAtLocalTime = localTime + interval

        status.postValue(Status.still(triggerAtLocalTime))
    }

    private fun handleActionExitFromStill(context: Context, elapsedTime: Long) {
        Log.d(javaClass.simpleName, "handleActionEnterToStill(elapsedTime: $elapsedTime)")
        cancelIntervention(context)

        val duration = with(App.boxStore.boxFor<PhysicalActivity>()) {
            val latestEvent = PhysicalActivity.latestActivity(this)
            if(latestEvent?.eventType != PhysicalActivity.TYPE_ACTIVE) {
                PhysicalActivity.active(this, elapsedTime, latestEvent)
                elapsedTime - (latestEvent?.startElapsedTimeMillis ?: Long.MAX_VALUE)
            } else {
                null
            }
        } ?: return

        status.postValue(Status.move())

        val configManager = ConfigManager.getInstance(context)
        val interval = TimeUnit.MINUTES.toMillis(configManager.interventionInitIntervalMin.toLong())

        if(interval <= duration && checkNotificationAvailable(configManager)) {
            Notifications.notifyStandUp(context, duration, buildDismissAction(context), buildSnoozeAction(context))
        }
    }

    private fun handleActionInterventionTrigger(context: Context, elapsedTime: Long) {
        Log.d(javaClass.simpleName, "handleActionInterventionTrigger(elapsedTime: $elapsedTime)")

        val configManager = ConfigManager.getInstance(context)
        val retryInterval = TimeUnit.MINUTES.toMillis(configManager.interventionRetryIntervalMin.toLong())
        val nowElapsedTime = SystemClock.elapsedRealtime()
        val duration = nowElapsedTime - elapsedTime

        if(checkNotificationAvailable(configManager)) {
            notifyIntervention(context, duration, buildDismissAction(context), buildSnoozeAction(context), buildStandUpAction(context))
        }

        val triggerAt = nowElapsedTime + retryInterval

        scheduleIntervention(context, triggerAt, elapsedTime)

        val localTime = elapsedTimeToLocalTime(nowElapsedTime)
        val triggerAtLocalTime = localTime + retryInterval

        status.postValue(Status.still(triggerAtLocalTime))
    }

    private fun handleActionSnooze(context: Context) {
        dismissIntervention(context)

        val configManager = ConfigManager.getInstance(context)
        val now = SystemClock.elapsedRealtime()
        val snoozeDelay = TimeUnit.MINUTES.toMillis(configManager.interventionSnoozeDurationMin.toLong())

        configManager.interventionSnoozeUntil = now + snoozeDelay
    }

    private fun handleActionDismiss(context: Context) {
        dismissIntervention(context)
    }

    private fun handleMockExitFromStill(context: Context) {
        dismissIntervention(context)
        cancelIntervention(context)

        val nowElapsedMillis = SystemClock.elapsedRealtime()

        with(App.boxStore.boxFor<PhysicalActivity>()) {
            val latestEvent = PhysicalActivity.latestActivity(this)
            
            if(latestEvent?.eventType != PhysicalActivity.TYPE_ACTIVE) {
                val threeMinBefore = nowElapsedMillis - TimeUnit.MINUTES.toMillis(3)
                val prevElapsedTime = if(latestEvent != null) Math.max(threeMinBefore, latestEvent.startElapsedTimeMillis) else threeMinBefore
                val newExitEvent = PhysicalActivity.active(this, prevElapsedTime, latestEvent)
                PhysicalActivity.sedentary(this, nowElapsedMillis, newExitEvent)
            } else {
                null
            }
        } ?: return
        Log.d(javaClass.simpleName, "handleMockExitFromStill")
        val configManager = ConfigManager.getInstance(context)
        val interval = TimeUnit.MINUTES.toMillis(configManager.interventionInitIntervalMin.toLong())
        val triggerAt = nowElapsedMillis + interval

        scheduleIntervention(context, triggerAt, nowElapsedMillis)

        val localTime = elapsedTimeToLocalTime(nowElapsedMillis)
        val triggerAtLocalTime = localTime + interval

        status.postValue(Status.still(triggerAtLocalTime))
    }

    private fun scheduleIntervention(context: Context, triggerAt: Long, sedentaryStartTime: Long) =
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarmIntent = Intent(ACTION_INTERVENTION_TRIGGER)
                .putExtra(EXTRA_SEDENTARY_START_TIME, sedentaryStartTime)
                .let {
                    PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_TRIGGER, it, PendingIntent.FLAG_UPDATE_CURRENT)
                }
            AlarmManagerCompat.setExactAndAllowWhileIdle(this, AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, alarmIntent)
        }

    private fun cancelIntervention(context: Context) =
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarmIntent = Intent(ACTION_INTERVENTION_TRIGGER).let {
                PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_TRIGGER, it, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            cancel(alarmIntent)
        }

    private fun checkNotificationAvailable(configManager: ConfigManager) : Boolean {
        val nowElapsedTime = SystemClock.elapsedRealtime()
        val nowDay = DayOfWeek.today()
        val nowLocalTime = LocalTime.now()

        val shouldSnooze = configManager.interventionShouldSnooze
        val snoozeUntil = configManager.interventionSnoozeUntil
        val days = configManager.interventionDaysOfWeek
        val timeRange = configManager.interventionDailyTimeRange

        Log.d(javaClass.simpleName, "nowElapsedTime = $nowElapsedTime, " +
                "shouldSnooze = $shouldSnooze, snoozeUntil = $snoozeUntil," +
                "nowDay = $nowDay, availableDays = $days, isInDays = ${nowDay in days}, nowLocalTime = $nowLocalTime, isInLocalTime = ${nowLocalTime in timeRange.first..timeRange.second}")

        return (!shouldSnooze && nowElapsedTime >= snoozeUntil && nowDay in days && nowLocalTime in timeRange.first..timeRange.second)
    }

    data class Status(val state: State, val triggerAt: Long? = null) {
        enum class State {
            UNKNOWN, STILL, MOVED
        }

        companion object {
            fun still(triggerAt: Long) = Status(State.STILL, triggerAt)
            fun move() = Status(State.MOVED)

            fun newLiveData() = MutableLiveData<Status>().apply { postValue(Status(State.UNKNOWN)) }
        }
    }

    companion object {
        private val EXTRA_SEDENTARY_START_TIME =
            "${EventHandler::class.java.name}.EXTRA_SEDENTARY_START_TIME"

        private val INTENT_FILTER = IntentFilter().apply {
            addAction(ACTION_TRANSITION_UPDATE)
            addAction(ACTION_INTERVENTION_SNOOZED)
            addAction(ACTION_INTERVENTION_TRIGGER)
            addAction(ACTION_INTERVENTION_DISMISS)
            addAction(ACTION_MOCK_EXIT_FROM_STILL)
        }

        private var instance: EventHandler? = null

        fun getInstance() : EventHandler {
            if(instance == null) {
                instance = EventHandler()
            }
            return instance!!
        }

        fun buildDismissAction(context: Context) = Intent(ACTION_INTERVENTION_DISMISS).let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_DISMISS, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }.let {
            NotificationCompat.Action(R.drawable.baseline_clear_black_24, context.getString(R.string.noti_action_dismiss), it)
        }

        fun buildStandUpAction(context: Context) = Intent(ACTION_MOCK_EXIT_FROM_STILL).let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_MOCK_EXIT_FROM_STILL, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }.let {
            NotificationCompat.Action(R.drawable.baseline_replay_black_24, context.getString(R.string.noti_action_already_stand_up), it)
        }

        fun buildSnoozeAction(context: Context) = Intent(ACTION_INTERVENTION_SNOOZED).let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_SNOOZED, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }.let {
            NotificationCompat.Action(R.drawable.baseline_snooze_black_24, context.getString(R.string.noti_action_snooze), it)
        }
    }
}