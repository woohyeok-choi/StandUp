package kr.ac.kaist.iclab.standup

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_REFRESH_WIDGET
import kr.ac.kaist.iclab.standup.common.ConfigManager
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.common.RequestCodes
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import java.util.concurrent.TimeUnit
import android.content.ComponentName

/**
 * Implementation of App Widget functionality.
 */
class SedentaryStatWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == ACTION_REFRESH_WIDGET && context != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidgetComponentName = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)

            onUpdate(context, appWidgetManager, appWidgetIds)
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val configManager = ConfigManager.getInstance(context)
        val dayStart = DateTimes.asDayStartMillis(System.currentTimeMillis())
        val (dailyFrom, dailyTo) = configManager.interventionDailyTimeRange
        val from = dayStart + dailyFrom.asOffsetMillis()
        val to = dayStart + dailyTo.asOffsetMillis()
        val views = RemoteViews(context.packageName, R.layout.widget_sedentary_stat)

        val stat = PhysicalActivity.statSedentary(App.boxStore.boxFor(), from, to)

        if(stat != null) {
            views.setTextViewText(R.id.txtWidgetSedentaryAvg,
                "${TimeUnit.MILLISECONDS.toMinutes(stat.avgDurationMillis)} ${context.getString(R.string.unit_minute)}")
            views.setTextViewText(R.id.txtWidgetTotalAvg,
                "${TimeUnit.MILLISECONDS.toMinutes(stat.totalDurationMillis)} ${context.getString(R.string.unit_minute)}"
            )
        } else {
            views.setTextViewText(R.id.txtWidgetSedentaryAvg, context.getString(R.string.general_none_collection))
            views.setTextViewText(R.id.txtWidgetTotalAvg, context.getString(R.string.general_none_collection))
        }

        val refreshIntent = Intent(context, SedentaryStatWidget::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }.let { intent ->
            PendingIntent.getBroadcast(context, RequestCodes.REQUEST_CODE_REFRESH_WIDGET, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        views.setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d(SedentaryStatWidget::class.java.simpleName, "updateComplete")
    }

}

