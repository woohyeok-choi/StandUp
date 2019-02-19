package kr.ac.kaist.iclab.standup.common

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import com.google.android.material.snackbar.Snackbar
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.background.AppUsageStatCollector
import kr.ac.kaist.iclab.standup.background.SyncManager
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_LAUNCH_FROM_NOTIFICATION
import kr.ac.kaist.iclab.standup.entity.AppUsageStats
import kr.ac.kaist.iclab.standup.foreground.activity.MainActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


private const val PACKAGE_NAME = "kr.ac.kaist.iclab.standup"

object Actions {
    const val ACTION_TRANSITION_UPDATE = "$PACKAGE_NAME.ACTION_TRANSITION_UPDATE"
    const val ACTION_MOCK_EXIT_FROM_STILL = "$PACKAGE_NAME.ACTION_MOCK_EXIT_FROM_STILL"

    const val ACTION_INTERVENTION_TRIGGER = "$PACKAGE_NAME.ACTION_INTERVENTION_TRIGGER"
    const val ACTION_INTERVENTION_SNOOZED = "$PACKAGE_NAME.ACTION_INTERVENTION_SNOOZED"
    const val ACTION_INTERVENTION_DISMISS = "$PACKAGE_NAME.ACTION_INTERVENTION_DISMISS"

    const val ACTION_PSEUDO_INTERVENTION_TRIGGER = "$PACKAGE_NAME.ACTION_PSEUDO_INTERVENTION_TRIGGER"
    const val ACTION_PSEUDO_EXIT_FROM_STILL = "$PACKAGE_NAME.ACTION_PSEUDO_EXIT_FROM_STILL"

    const val ACTION_REFRESH_WIDGET = "$PACKAGE_NAME.ACTION_REFRESH_WIDGET"

}

object RequestCodes {
    const val REQUEST_CODE_TRANSITION_UPDATE = 0x0002
    const val REQUEST_CODE_MOCK_EXIT_FROM_STILL = 0x0003

    const val REQUEST_CODE_INTERVENTION_TRIGGER = 0x0004
    const val REQUEST_CODE_INTERVENTION_SNOOZED = 0x0005
    const val REQUEST_CODE_INTERVENTION_DISMISS = 0x0006

    const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x0007
    const val REQUEST_CODE_LAUNCH_FROM_NOTIFICATION = 0x0008

    const val REQUEST_CODE_REFRESH_WIDGET = 0x0009
}

object Notifications {
    private const val ID_NOTIFICATION_INTERVENTION_CHANNEL = "$PACKAGE_NAME.ID_NOTIFICATION_INTERVENTION_CHANNEL"
    private const val ID_NOTIFICATION_SILENT_CHANNEL = "$PACKAGE_NAME.ID_NOTIFICATION_SILENT_CHANNEL"

    private const val ID_NOTIFICATION_INTERVENTION = 0x0001
    private const val ID_NOTIFICATION_SERVICE_RUNNING = 0x0003

    private fun createInterventionChannel(context: Context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = context.getString(R.string.noti_intervention_channel_name)
            val desc = context.getString(R.string.noti_intervention_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(ID_NOTIFICATION_INTERVENTION_CHANNEL, name, importance).apply {
                description = desc
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSilentChannel(context: Context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = context.getString(R.string.noti_silent_channel_name)
            val desc = context.getString(R.string.noti_silent_channel_desc)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(ID_NOTIFICATION_SILENT_CHANNEL, name, importance).apply {
                description = desc
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                enableLights(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notify(context: Context, notification: Notification, id: Int, isForeground: Boolean = false) {
        if(isForeground && context is Service) {
            context.startForeground(id, notification)
        } else {
            with(NotificationManagerCompat.from(context)) { notify(id, notification) }
        }
    }

    private fun dismiss(context: Context, id: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(id)
        }
    }

    private fun buildContentIntent(context: Context): PendingIntent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .let {
                PendingIntent.getActivity(context, REQUEST_CODE_LAUNCH_FROM_NOTIFICATION, it, PendingIntent.FLAG_UPDATE_CURRENT)
            }

    fun dismissIntervention(context: Context) {
        dismiss(context, ID_NOTIFICATION_INTERVENTION)
    }

    fun notifyIntervention(context: Context, sedentaryDurationMillis: Long, vararg actions: NotificationCompat.Action) {
        createInterventionChannel(context)

        val title = context.getString(R.string.noti_sedentariness_title)
        val text = "${DateTimes.formatDuration(context, sedentaryDurationMillis, false)} ${context.getString(R.string.noti_sedentariness_text)}"
        val contentIntent = buildContentIntent(context)
        val notification = NotificationCompat.Builder(context, Notifications.ID_NOTIFICATION_INTERVENTION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_sedentary)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setColor(context.getColor(R.color.primary))
            .setShowWhen(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { actions.forEach { addAction(it) } }
            .build()

        notify(context, notification, Notifications.ID_NOTIFICATION_INTERVENTION)
    }

    fun notifyStandUp(context: Context, sedentaryDurationMillis: Long, vararg actions: NotificationCompat.Action) {
        createInterventionChannel(context)

        val title = context.getString(R.string.noti_stand_up_title)
        val text = "${DateTimes.formatDuration(context, sedentaryDurationMillis, false)} ${context.getString(R.string.noti_stand_up_text)}"
        val contentIntent = buildContentIntent(context)
        val notification = NotificationCompat.Builder(context, Notifications.ID_NOTIFICATION_INTERVENTION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_standup)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(context.getColor(R.color.primary))
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply { actions.forEach { addAction(it) } }
            .build()

        notify(context, notification, Notifications.ID_NOTIFICATION_INTERVENTION)
    }

    fun notifyServiceRunning(context: Context, vararg actions: NotificationCompat.Action) {
        createSilentChannel(context)

        val title = context.getString(R.string.noti_service_running_title)
        val text = context.getString(R.string.noti_service_running_text)
        val notification = NotificationCompat.Builder(context, Notifications.ID_NOTIFICATION_SILENT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_sedentary)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(context.getColor(R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .apply { actions.forEach { addAction(it) } }
            .build()

        notify(context, notification, Notifications.ID_NOTIFICATION_SERVICE_RUNNING, true)
    }
}

object Permissions {
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.RECEIVE_BOOT_COMPLETED
    )

    fun checkPermission(context: Context): Boolean = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun requestPermission(context: Context, onComplete: (isAlreadyGranted: Boolean, isGranted: Boolean) -> Unit) {
        if(checkPermission(context)) {
            onComplete(true, true)
            return
        }

        TedPermission.with(context)
            .setPermissions(*REQUIRED_PERMISSIONS)
            .setPermissionListener(
                object : PermissionListener {
                    override fun onPermissionGranted() {
                        onComplete(false, true)
                    }

                    override fun onPermissionDenied(deniedPermissions: ArrayList<String>?) {
                        Log.d(javaClass.name, deniedPermissions?.joinToString(","))
                        onComplete(false, false)
                    }
                }
            ).check()
    }
}

object Messages {
    fun showToast(context: Context, message: CharSequence) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showToast(context: Context, message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showSnackBar(view: View, message: CharSequence, actionText: CharSequence? = null, action: (() -> Unit)? = null) {
        val snackBar = if (actionText != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction(actionText) {
                action?.invoke()
            }
        } else {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        }
        snackBar.show()
    }

    fun showSnackBar(view: View, message: Int, actionText: Int? = null, action: (() -> Unit)? = null) {
        showSnackBar(view, view.context.getString(message), actionText?.let { view.context.getString(it) }, action)
    }
}

object DateTimes {
    private val COMPACT_FORMATTER = SimpleDateFormat("MM.dd", Locale.US)
    private val DEFAULT_FORMATTER = SimpleDateFormat("yy.MM.dd HH.mm.ss", Locale.US)

    fun formatDateTime(millis: Long) = DEFAULT_FORMATTER.format(GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = millis }.time)

    fun formatDateTime(context: Context, millis: Long) = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)

    fun formatTimeRange(context: Context, startMillis: Long, endMillis: Long) : String {
        val now = System.currentTimeMillis()
        val tempMillis = if(endMillis == 0L) now else endMillis
        val isSameDay = isSameDay(startMillis, tempMillis)

        val sameDayFlag = DateUtils.FORMAT_SHOW_TIME
        val nonSameDayFlag = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH

        val fromStr = if(isSameDay) {
            DateUtils.formatDateTime(context, startMillis, sameDayFlag)
        } else {
            DateUtils.formatDateTime(context, startMillis, nonSameDayFlag)
        }

        val toStr = when {
            endMillis == 0L -> context.getString(R.string.general_now)
            isSameDay -> DateUtils.formatDateTime(context, endMillis, sameDayFlag)
            else -> DateUtils.formatDateTime(context, endMillis, nonSameDayFlag)
        }

        return "$fromStr - $toStr"
    }

    fun formatCompactDate(millis: Long): String = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        }.let {
            COMPACT_FORMATTER.format(it.time)
        }

    fun formatDayAgo(context: Context, millis: Long, now: Long): String = if(diffDays(millis, now) < 3) {
        DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    } else {
        formatDate(context, millis)
    }

    fun formatDate(context: Context, millis: Long) = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE)

    fun formatWeekday(context: Context, millis: Long) : String =
        DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)

    fun isSameDay(one: Long, another: Long): Boolean {
        val cal1 = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = one }
        val cal2 = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = another }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun diffDays(one: Long, another: Long): Int {
        val oneDay = asDayStartMillis(one)
        val anotherDay = asDayStartMillis(another)
        return TimeUnit.MILLISECONDS.toDays(Math.abs(oneDay - anotherDay)).toInt()
    }

    fun formatDuration(context: Context, millis: Long, showSecond: Boolean = true) : String {
        var tempMillis = millis

        val hourUnit = context.getString(R.string.unit_hour)
        val minuteUnit = context.getString(R.string.unit_minute)
        val secondUnit = context.getString(R.string.unit_second)

        val hourMillis = TimeUnit.HOURS.toMillis(1)
        val minuteMillis = TimeUnit.MINUTES.toMillis(1)
        val secondMillis = TimeUnit.SECONDS.toMillis(1)

        var hours = 0L
        var minutes = 0L
        var seconds = 0L

        if(tempMillis >= hourMillis) {
            hours = tempMillis / hourMillis
            tempMillis -= hours * hourMillis
        }

        if(tempMillis >= minuteMillis) {
            minutes = tempMillis / minuteMillis
            tempMillis -= minutes * minuteMillis
        }

        if(tempMillis >= secondMillis) {
            seconds = tempMillis / secondMillis
        }

        return if(hours > 0) {
            "$hours$hourUnit $minutes$minuteUnit"
        } else if(showSecond) {
            "$minutes$minuteUnit $seconds$secondUnit"
        } else {
            "$minutes$minuteUnit"
        }
    }

    fun asDayStartMillis(millis: Long) = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun elapsedTimeToLocalTime(target: Long) : Long {
        val diff = SystemClock.elapsedRealtime() - target
        return System.currentTimeMillis() - diff
    }

    fun localTimeToElapsedTime(target: Long) : Long {
        val diff = System.currentTimeMillis() - target
        return SystemClock.elapsedRealtime() - diff
    }
}

object WorkerUtil {
    inline fun <reified T: Worker> periodicWork(interval: Long, unit: TimeUnit) {
        val request = PeriodicWorkRequestBuilder<T>(interval, unit).build()
        WorkManager.getInstance().enqueueUniquePeriodicWork(T::class.java.simpleName, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    inline fun <reified T: Worker> cancel() {
        WorkManager.getInstance().cancelUniqueWork(T::class.java.simpleName)
    }
}
