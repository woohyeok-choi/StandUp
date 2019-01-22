package kr.ac.kaist.iclab.standup.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_INTERVENTION_SNOOZED
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_LOCATION_UPDATE
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_TRANSITION_UPDATE
import kr.ac.kaist.iclab.standup.common.DayOfWeek
import kr.ac.kaist.iclab.standup.common.LocalTime
import kr.ac.kaist.iclab.standup.common.Notifications
import kr.ac.kaist.iclab.standup.common.Notifications.dismissIntervention
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_INTERVENTION_SNOOZED
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.data.Location
import kr.ac.kaist.iclab.standup.data.Event
import kr.ac.kaist.iclab.standup.data.Event_
import kr.ac.kaist.iclab.standup.util.ConfigManager
import java.util.concurrent.TimeUnit

class EventHandler : BroadcastReceiver() {
    init {
        status.postValue(Status(State.UNKNOWN))
    }

    enum class State {
        UNKNOWN, STILL, MOVED
    }

    val intentFilter = IntentFilter().apply {
        addAction(ACTION_TRANSITION_UPDATE)
        addAction(ACTION_INTERVENTION_SNOOZED)
        addAction(ACTION_LOCATION_UPDATE)
        addAction(ACTION_INTERVENTION_TRIGGER)
    }

    data class Status(val state: State, val triggerAt: Long? = null)

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
                    val nowElapsedTime = SystemClock.elapsedRealtime()
                    val nowLocalTime = System.currentTimeMillis()
                    val elapsedTime = TimeUnit.NANOSECONDS.toMillis(it.elapsedRealTimeNanos)
                    val localTime = nowLocalTime - (nowElapsedTime - elapsedTime)

                    if(it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        handleActionEnterToStill(context, elapsedTime, localTime)
                    } else {
                        handleActionExitFromStill(context, elapsedTime, localTime)
                    }
                }
            }
            ACTION_LOCATION_UPDATE -> handleActionLocationUpdate(intent)
            ACTION_INTERVENTION_TRIGGER -> if(intent.hasExtra(EXTRA_SEDENTARY_START_LOCAL_TIME)
                && intent.hasExtra(EXTRA_SEDENTARY_START_ELAPSED_TIME)) {
                handleActionInterventionTrigger(context,
                    intent.getLongExtra(EXTRA_SEDENTARY_START_ELAPSED_TIME, 0L),
                    intent.getLongExtra(EXTRA_SEDENTARY_START_LOCAL_TIME, 0L)
                )
            }
            ACTION_INTERVENTION_SNOOZED -> handleActionSnooze(context)
        }
    }

    fun handleActionEnterToStill(context: Context, originalEnterElapsedTime: Long, originalEnterLocalTime: Long) {
        Log.d(javaClass.simpleName,
            "handleActionEnterToStill(" +
                    "originalEnterElapsedTime: $originalEnterElapsedTime, " +
                    "originalEnterLocalTime: $originalEnterLocalTime)"
        )

        val configManager = ConfigManager.getInstance(context)
        val alarmIntent = buildInterventionIntent(context, originalEnterElapsedTime, originalEnterLocalTime)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delay = TimeUnit.MINUTES.toMillis(configManager.interventionInitIntervalMin.toLong())
        val box = App.boxStore.boxFor<Event>()

        box.put(
            Event(startElapsedTime = originalEnterElapsedTime, startLocalTime = originalEnterLocalTime)
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, originalEnterElapsedTime + delay, alarmIntent)

        status.postValue(
            Status(State.STILL, originalEnterLocalTime + delay)
        )
    }

    fun handleActionInterventionTrigger(context: Context, originalEnterElapsedTime: Long, originalEnterLocalTime: Long) {
        Log.d(javaClass.simpleName,
            "handleActionInterventionTrigger(" +
                    "originalEnterElapsedTime: $originalEnterElapsedTime, " +
                    "originalEnterLocalTime: $originalEnterLocalTime)"
        )

        val configManager = ConfigManager.getInstance(context)
        val alarmIntent = buildInterventionIntent(context, originalEnterElapsedTime, originalEnterLocalTime)
        val delayIntent = Intent(ACTION_INTERVENTION_SNOOZED).let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_SNOOZED, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val delayAction = NotificationCompat.Action(R.drawable.baseline_snooze_black_24,
            context.getString(R.string.noti_sedentariness_action_delayed),
            delayIntent
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delay = TimeUnit.MINUTES.toMillis(configManager.interventionInitIntervalMin.toLong())
        val retryDelay = TimeUnit.MINUTES.toMillis(configManager.interventionRetryIntervalMin.toLong())
        val nowElapsedTime = SystemClock.elapsedRealtime()
        val nowLocalTime = System.currentTimeMillis()
        val duration = nowElapsedTime - originalEnterElapsedTime

        if(duration >= delay && checkNotificationAvailable(configManager)) {
            Notifications.notifyIntervention(context, duration, delayAction)
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, nowElapsedTime + retryDelay, alarmIntent)

        status.postValue(
            Status(State.STILL, nowLocalTime + retryDelay)
        )
    }

    fun handleActionSnooze(context: Context) {
        val configManager = ConfigManager.getInstance(context)
        val now = SystemClock.elapsedRealtime()
        val snoozeDelay = TimeUnit.MINUTES.toMillis(configManager.interventionSnoozeDurationMin.toLong())

        configManager.interventionSnoozeUntil = now + snoozeDelay

        dismissIntervention(context)
    }

    fun handleActionExitFromStill(context: Context, originalExitElapsedTime: Long, originalExitLocalTime: Long) {
        val configManager = ConfigManager.getInstance(context)
        val interventionIntent = buildInterventionIntent(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delayIntent = Intent(ACTION_INTERVENTION_SNOOZED).let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_SNOOZED, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val delayAction = NotificationCompat.Action(R.drawable.baseline_snooze_black_24,
            context.getString(R.string.noti_sedentariness_action_delayed),
            delayIntent
        )
        val delay = TimeUnit.MINUTES.toMinutes(configManager.interventionInitIntervalMin.toLong())
        val box = App.boxStore.boxFor<Event>()
        val latestSedentaryStat = box.query()
            .less(Event_.startElapsedTime, originalExitElapsedTime)
            .greater(Event_.startElapsedTime, 0)
            .equal(Event_.isUpdated, false)
            .orderDesc(Event_.startElapsedTime)
            .build()
            .findFirst()

        if(latestSedentaryStat != null) {
            box.put(
                latestSedentaryStat.apply {
                    endElapsedTime = originalExitElapsedTime
                    endLocalTime = originalExitLocalTime
                    isUpdated = true
                }
            )

            val duration = originalExitElapsedTime - latestSedentaryStat.startElapsedTime
            if(duration >= delay && checkNotificationAvailable(configManager)) {
                Notifications.notifyStandUp(context, delayAction)
            }
        }

        alarmManager.cancel(interventionIntent)

        status.postValue(Status(State.MOVED))
    }

    fun handleActionLocationUpdate(locationIntent: Intent?) {
        val box = App.boxStore.boxFor<Location>()
        if(locationIntent != null && LocationResult.hasResult(locationIntent)) {
            val result = LocationResult.extractResult(locationIntent)

            box.put(
                Location(
                    elapsedTime = TimeUnit.NANOSECONDS.toMillis(result.lastLocation.elapsedRealtimeNanos),
                    localTime = result.lastLocation.time,
                    latitude = result.lastLocation.latitude,
                    longitude = result.lastLocation.longitude,
                    accuracy = result.lastLocation.accuracy
                )
            )
        }
    }

    fun buildInterventionIntent(context: Context, elapsedTime: Long? = null, localTime: Long? = null) = Intent(ACTION_INTERVENTION_TRIGGER)
        .putExtra(EXTRA_SEDENTARY_START_ELAPSED_TIME, elapsedTime ?: 0L)
        .putExtra(EXTRA_SEDENTARY_START_LOCAL_TIME, localTime ?: 0L)
        .let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_INTERVENTION_TRIGGER, it, PendingIntent.FLAG_UPDATE_CURRENT)
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

    companion object {
        private val EXTRA_SEDENTARY_START_LOCAL_TIME =
            "${EventHandler::class.java.name}.EXTRA_SEDENTARY_START_LOCAL_TIME"
        private val EXTRA_SEDENTARY_START_ELAPSED_TIME =
            "${EventHandler::class.java.name}.EXTRA_SEDENTARY_START_ELAPSED_TIME"

        val status = MutableLiveData<Status>()
    }
}