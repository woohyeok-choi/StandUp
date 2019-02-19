package kr.ac.kaist.iclab.standup.entity

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.Box
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import java.lang.Exception
import java.util.concurrent.TimeUnit

@Entity
data class AppUsageStats(
    @Id var id: Long = 0L,
    val queryTime: Long,
    var email: String = "",
    val name: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val lastTimeTimeUsed: Long,
    val totalTimeForeground: Long,
    var isExported: Boolean = false
) {
    companion object {
        val INTERVAL = TimeUnit.HOURS.toMillis(3)

        fun query(box: Box<AppUsageStats>, context: Context) {
            val now = System.currentTimeMillis()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val packageManager = context.packageManager
            val lastQueryTime = box.query().orderDesc(AppUsageStats_.queryTime).build().findFirst()?.queryTime ?: now - INTERVAL

            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, lastQueryTime, now).map { stat ->
                AppUsageStats(
                    queryTime = now,
                    email = FirebaseAuth.getInstance().currentUser?.email ?: "UNKNOWN",
                    name = try { packageManager.getApplicationInfo(stat.packageName, PackageManager.GET_META_DATA)?.let { info ->
                            packageManager.getApplicationLabel(info)?.toString()
                        } ?: "UNKNOWN" } catch (e: Exception) {"UNKNOWN"},
                    packageName = stat.packageName,
                    startTime = stat.firstTimeStamp,
                    endTime = stat.lastTimeStamp,
                    lastTimeTimeUsed = stat.lastTimeUsed,
                    totalTimeForeground = stat.totalTimeInForeground
                )
            }.let {
                box.put(it)
            }
        }

        fun checkEnabled(context: Context) : Boolean {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }

        val setupIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
}