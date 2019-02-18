package kr.ac.kaist.iclab.standup.foreground.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.BuildConfig
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.background.ActivityTransitionCollector
import kr.ac.kaist.iclab.standup.background.AppUsageStatCollector
import kr.ac.kaist.iclab.standup.background.EventHandler
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_PSEUDO_EXIT_FROM_STILL
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_PSEUDO_INTERVENTION_TRIGGER
import kr.ac.kaist.iclab.standup.common.ConfigManager
import kr.ac.kaist.iclab.standup.common.Messages
import kr.ac.kaist.iclab.standup.common.WorkerUtil
import kr.ac.kaist.iclab.standup.entity.AppUsageStats
import kr.ac.kaist.iclab.standup.entity.EventLog
import kr.ac.kaist.iclab.standup.foreground.activity.RootActivity

class ConfigFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        context?.let {
            val box = App.boxStore.boxFor<EventLog>()

            EventLog.new(
                box, "Interaction", "Config changed", ConfigManager.getInstance(it).toPrettyPrint()
            )
        }
    }

    override fun onStart() {
        super.onStart()

        EventLog.new(App.boxStore.boxFor(), "Interaction", "ConfigFragment", mapOf("Started" to true))
    }

    override fun onStop() {
        super.onStop()

        EventLog.new(App.boxStore.boxFor(), "Interaction", "ConfigFragment", mapOf("Started" to false))
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        context?.let {
            findPreference(getString(R.string.pref_etc_app_usage))?.summary = if(AppUsageStats.checkEnabled(it)) {
                getString(R.string.general_accepted)
            } else {
                getString(R.string.general_rejected)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        if(BuildConfig.DEBUG_MODE) {
            initDebugMode()
        }

        val snoozeUntilPreference = findPreference(getString(R.string.pref_status_intervention_snooze_until))
        val collectorStatusPreference = findPreference(getString(R.string.pref_status_collector))
        val sedentaryStatusPreference = findPreference(getString(R.string.pref_status_sedentariness))
        val accountStatusPreference = findPreference(getString(R.string.pref_status_account))

        context?.let { context ->
            val configManager = ConfigManager.getInstance(context)
            accountStatusPreference.summary = FirebaseAuth.getInstance().currentUser?.email ?: getString(R.string.general_unknown)
            updateSnoozeUntil(context, configManager, snoozeUntilPreference)
            observeCollectorStatus(collectorStatusPreference)
            observeSedentaryStatus(context, sedentaryStatusPreference)

        }
    }

    private fun updateSnoozeUntil(context: Context, configManager: ConfigManager, preference: Preference) {
        val snoozeUntil = configManager.interventionSnoozeUntil
        val nowElapsedMillis = SystemClock.elapsedRealtime()
        val nowLocalTime = System.currentTimeMillis()
        val diff = nowElapsedMillis - snoozeUntil

        if(diff >= 0) {
            preference.setSummary(R.string.pref_status_intervention_snooze_until_summary_off)
        } else {
            val untilLocalTime = nowLocalTime - diff
            val dateString = DateUtils.formatDateTime(context, untilLocalTime,
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
            preference.summary = "- $dateString"
        }
    }

    private fun observeCollectorStatus(preference: Preference) {
        ActivityTransitionCollector.getInstance().status.observe(this, Observer {
            val error = it.error?.localizedMessage?.let { msg -> "($msg)" } ?: ""
            val summary = "${it.state.name} $error"

            preference.summary = summary
        })
    }

    private fun observeSedentaryStatus(context: Context, preference: Preference) {
        EventHandler.getInstance().status.observe(this, Observer {
            val triggerAt = it.triggerAt?.let { triggerAt ->
                DateUtils.formatDateTime(context, triggerAt, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
            }?.let { msg ->
                "(다음 알림 전달 시간: $msg)"
            } ?: ""
            val summary = "${it.state.name} $triggerAt"
            preference.summary = summary
        })
    }

    private fun initDebugMode() {
        addPreferencesFromResource(R.xml.debug_preference)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key) {
            getString(R.string.pref_intervention_debug_action_intervention) ->
                preference.context?.sendBroadcast(Intent(ACTION_PSEUDO_INTERVENTION_TRIGGER))
            getString(R.string.pref_intervention_debug_action_stand_up) ->
                preference.context?.sendBroadcast(Intent(ACTION_PSEUDO_EXIT_FROM_STILL))
            getString(R.string.pref_etc_app_usage) -> {
                preference.context?.let { Messages.showToast(it, R.string.msg_normal_request_app_usage_collect) }
                startActivity(AppUsageStats.setupIntent)
            }
            getString(R.string.pref_etc_sign_out) -> {
                FirebaseAuth.getInstance().signOut()
                WorkerUtil.cancel<AppUsageStatCollector>()

                activity?.let {
                    startActivity(Intent(it, RootActivity::class.java))
                    it.finish()
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    companion object {
        fun newInstance() = ConfigFragment()
    }
}