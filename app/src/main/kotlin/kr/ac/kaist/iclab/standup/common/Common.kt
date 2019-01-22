package kr.ac.kaist.iclab.standup.common

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kr.ac.kaist.iclab.standup.R
import java.util.ArrayList
import java.util.concurrent.TimeUnit


private const val PACKAGE_NAME = "kr.ac.kaist.iclab.standup"

object Actions {
   const val ACTION_LOCATION_UPDATE = "$PACKAGE_NAME.ACTION_LOCATION_UPDATE"
    const val ACTION_TRANSITION_UPDATE = "$PACKAGE_NAME.ACTION_TRANSITION_UPDATE"

    const val ACTION_INTERVENTION_TRIGGER = "$PACKAGE_NAME.ACTION_INTERVENTION_TRIGGER"
    const val ACTION_INTERVENTION_SNOOZED = "$PACKAGE_NAME.ACTION_INTERVENTION_SNOOZED"
}

object RequestCodes {
    const val REQUEST_CODE_LOCATION_UPDATE = 0x0001
    const val REQUEST_CODE_TRANSITION_UPDATE = 0x0002
    const val REQUEST_CODE_INTERVENTION_TRIGGER = 0x0003
    const val REQUEST_CODE_INTERVENTION_SNOOZED = 0x0006
    const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x0007
}

object Notifications {
    const val ID_NOTIFICATION_INTERVENTION_CHANNEL = "$PACKAGE_NAME.ID_NOTIFICATION_INTERVENTION_CHANNEL"
    const val ID_NOTIFICATION_SILENT_CHANNEL = "$PACKAGE_NAME.ID_NOTIFICATION_SILENT_CHANNEL"

    const val ID_NOTIFICATION_INTERVENTION = 0x0001
    const val ID_NOTIFICATION_SERVICE_RUNNING = 0x0003

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
            val importance = NotificationManager.IMPORTANCE_NONE
            val channel = NotificationChannel(ID_NOTIFICATION_SILENT_CHANNEL, name, importance).apply {
                description = desc
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
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

    fun dismissIntervention(context: Context) {
        dismiss(context, ID_NOTIFICATION_INTERVENTION)
    }

    fun notifyIntervention(context: Context, sedentaryDurationMillis: Long, vararg actions: NotificationCompat.Action) {
        createInterventionChannel(context)

        val title = "${TimeUnit.MILLISECONDS.toMinutes(sedentaryDurationMillis)}${context.getString(R.string.noti_sedentariness_title)}"
        val text = context.getString(R.string.noti_sedentariness_text)
        val notification = NotificationCompat.Builder(context, Notifications.ID_NOTIFICATION_INTERVENTION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_sedentary)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(context.getColor(R.color.primary))
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { actions.forEach { addAction(it) } }.build()

        notify(context, notification, Notifications.ID_NOTIFICATION_INTERVENTION)

    }

    fun notifyStandUp(context: Context, vararg actions: NotificationCompat.Action) {
        createInterventionChannel(context)

        val title = context.getString(R.string.noti_stand_up_title)
        val text = context.getString(R.string.noti_stand_up_text)
        val notification = NotificationCompat.Builder(context, Notifications.ID_NOTIFICATION_INTERVENTION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_standup)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(context.getColor(R.color.primary))
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply { actions.forEach { addAction(it) } }
            .build()

        notify(context, notification, Notifications.ID_NOTIFICATION_INTERVENTION)

    }

    fun notifyServiceRunning(context: Context) {
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
            .build()

        notify(context, notification, Notifications.ID_NOTIFICATION_SERVICE_RUNNING, true)
    }
}

object Permissions {
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
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
