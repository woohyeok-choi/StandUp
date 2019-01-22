package kr.ac.kaist.iclab.standup.util

import android.content.Context
import androidx.preference.PreferenceManager
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.DayOfWeek
import kr.ac.kaist.iclab.standup.foreground.preference.DaysOfWeekPreference
import kr.ac.kaist.iclab.standup.foreground.preference.LocalTimeRangePreference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ConfigManager private constructor(context: Context) {
    private var pref = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private var instance: ConfigManager? = null

        fun getInstance(context: Context) = (instance
            ?: ConfigManager(context))!!
    }

    var interventionInitIntervalMin by IntDelegate(
        context.getString(R.string.pref_intervention_init_interval), 60
    )

    var interventionRetryIntervalMin by IntDelegate(
        context.getString(R.string.pref_intervention_retry_interval), 15
    )

    var interventionSnoozeDurationMin by IntDelegate(
        context.getString(R.string.pref_intervention_snooze_duration), 60
    )

    var interventionShouldSnooze by BooleanDelegate(
        context.getString(R.string.pref_intervention_should_snooze), false
    )

    var interventionDailyTimeRange by LocalTimeRangePreference.Delegate(
        pref, context.getString(R.string.pref_intervention_daily_time_range), LocalTimeRangePreference.DEFAULT_VALUE
    )

    var interventionDaysOfWeek by DaysOfWeekPreference.Delegate(
        pref, context.getString(R.string.pref_intervention_days_of_week), DaysOfWeekPreference.DEFAULT_VALUE
    )

    var interventionSnoozeUntil by LongDelegate(
        context.getString(R.string.pref_intervention_snooze_until), 0
    )

    class IntDelegate(private val key: String, private val default: Int) : ReadWriteProperty<ConfigManager, Int> {
        override fun getValue(thisRef: ConfigManager, property: KProperty<*>): Int = thisRef.pref.getInt(key, default)

        override fun setValue(thisRef: ConfigManager, property: KProperty<*>, value: Int) = thisRef.pref.edit().putInt(key, value).apply()
    }

    class LongDelegate(private val key: String, private val default: Long) : ReadWriteProperty<ConfigManager, Long> {
        override operator fun getValue(thisRef: ConfigManager, property: KProperty<*>): Long = thisRef.pref.getLong(key, default)

        override operator fun setValue(thisRef: ConfigManager, property: KProperty<*>, value: Long) = thisRef.pref.edit().putLong(key, value).apply()
    }

    class BooleanDelegate(private val key: String, private val default: Boolean) : ReadWriteProperty<ConfigManager, Boolean> {
        override operator fun getValue(thisRef: ConfigManager, property: KProperty<*>): Boolean = thisRef.pref.getBoolean(key, default)

        override operator fun setValue(thisRef: ConfigManager, property: KProperty<*>, value: Boolean) = thisRef.pref.edit().putBoolean(key, value).apply()
    }

}