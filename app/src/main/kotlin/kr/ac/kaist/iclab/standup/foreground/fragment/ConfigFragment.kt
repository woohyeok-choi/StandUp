package kr.ac.kaist.iclab.standup.foreground.fragment

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.background.ActivityTransitionCollector
import kr.ac.kaist.iclab.standup.background.EventHandler
import kr.ac.kaist.iclab.standup.util.ConfigManager

class ConfigFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        val snoozeUntilPreference = findPreference(getString(R.string.pref_intervention_snooze_until))
        val collectorStatusPreference = findPreference(getString(R.string.pref_status_collector))
        val sedentaryStatusPreference = findPreference(getString(R.string.pref_status_sedentariness))


        context?.let {
            val configManager = ConfigManager.getInstance(it)
            updateSnoozeUntil(it, configManager, snoozeUntilPreference)
            observeCollectorStatus(collectorStatusPreference)
            observeSedentaryStatus(it, sedentaryStatusPreference)
        }
    }

    private fun updateSnoozeUntil(context: Context, configManager: ConfigManager, preference: Preference) {
        val snoozeUntil = configManager.interventionSnoozeUntil
        val nowElapsedMillis = SystemClock.elapsedRealtime()
        val nowLocalTime = System.currentTimeMillis()
        val diff = nowElapsedMillis - snoozeUntil

        if(diff >= 0) {
            preference.setSummary(R.string.pref_intervention_snooze_until_summary_off)
        } else {
            val untilLocalTime = nowLocalTime - diff
            val dateString = DateUtils.formatDateTime(context, untilLocalTime,
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
            preference.summary = "- $dateString"
        }
    }

    private fun observeCollectorStatus(preference: Preference) {
        ActivityTransitionCollector.status.observe(this, Observer {
            val error = it.error?.localizedMessage?.let { msg -> "($msg)" } ?: ""
            val summary = "${it.state.name} $error"

            preference.summary = summary
        })
    }

    private fun observeSedentaryStatus(context: Context, preference: Preference) {
        EventHandler.status.observe(this, Observer {
            val triggerAt = it.triggerAt?.let { triggerAt ->
                DateUtils.formatDateTime(context, triggerAt, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
            }?.let { msg ->
                "(다음 알림 전달 시간: $msg)"
            } ?: ""
            val summary = "${it.state.name} $triggerAt"
            preference.summary = summary
        })
    }
}