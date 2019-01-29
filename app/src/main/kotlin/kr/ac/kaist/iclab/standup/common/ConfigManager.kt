package kr.ac.kaist.iclab.standup.common

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.preference.PreferenceManager
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.foreground.preference.DaysOfWeekPreference
import kr.ac.kaist.iclab.standup.foreground.preference.LocalTimeRangePreference
import kr.ac.kaist.iclab.standup.foreground.preference.SeekBarPreference
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ConfigManager private constructor(context: Context) {
    private var pref = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private var instance: ConfigManager? = null

        fun getInstance(context: Context) : ConfigManager {
            if(instance == null) {
                instance =
                        ConfigManager(context)
            }
            return instance!!
        }
    }

    /**
     * Preferences which are directly set by a user's input.
     */
    val interventionInitIntervalMin by SeekBarPreference.ReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_init_interval), 60
    )

    val interventionRetryIntervalMin by SeekBarPreference.ReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_retry_interval), 15
    )

    val interventionSnoozeDurationMin by SeekBarPreference.ReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_snooze_duration), 60
    )

    val interventionShouldSnooze by BooleanReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_should_snooze), false
    )

    val interventionDailyTimeRange by LocalTimeRangePreference.ReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_daily_time_range), LocalTimeRangePreference.DEFAULT_VALUE
    )

    val interventionDaysOfWeek by DaysOfWeekPreference.ReadOnlyDelegate(
        pref, context.getString(R.string.pref_intervention_days_of_week), DaysOfWeekPreference.DEFAULT_VALUE
    )

    /**
     * Preferences which are internally set
     */
    var interventionSnoozeUntil by LongReadWriteDelegate(
        pref, context.getString(R.string.pref_intervention_snooze_until), 0
    )

    fun toPrettyPrint() : String {
        return "nowElapsedTime = ${SystemClock.elapsedRealtime()}, " +
                "interventionInitIntervalMin = $interventionInitIntervalMin, " +
                "interventionRetryIntervalMin = $interventionRetryIntervalMin, " +
                "interventionSnoozeDurationMin = $interventionSnoozeDurationMin, " +
                "interventionSnoozeUntil = $interventionSnoozeUntil, " +
                "interventionShouldSnooze = $interventionShouldSnooze, " +
                "interventionDailyTimeRange = $interventionDailyTimeRange, " +
                "interventionDaysOfWeek = $interventionDaysOfWeek"
    }


    class LongReadOnlyDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Long) : ReadOnlyProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long = sharedPreferences.getLong(key, default)
    }

    class LongReadWriteDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Long) : ReadWriteProperty<Any?, Long> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): Long = sharedPreferences.getLong(key, default)

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) = sharedPreferences.edit().putLong(key, value).apply()
    }

    class BooleanReadOnlyDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Boolean) : ReadOnlyProperty<Any?, Boolean> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = sharedPreferences.getBoolean(key, default)
    }

    class BooleanReadWriteDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = sharedPreferences.getBoolean(key, default)

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = sharedPreferences.edit().putBoolean(key, value).apply()
    }

}