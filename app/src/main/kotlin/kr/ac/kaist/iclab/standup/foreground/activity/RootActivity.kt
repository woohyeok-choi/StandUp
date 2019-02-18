package kr.ac.kaist.iclab.standup.foreground.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.BuildConfig
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.background.AppUsageStatCollector
import kr.ac.kaist.iclab.standup.background.SyncManager
import kr.ac.kaist.iclab.standup.common.TestGenerator
import kr.ac.kaist.iclab.standup.common.WorkerUtil
import java.util.concurrent.TimeUnit

class RootActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(javaClass.simpleName, "onCreate()")

        if(BuildConfig.DEBUG_MODE) {
            TestGenerator.generatePhysicalActivities(100, App.boxStore.boxFor())
        }

        PreferenceManager.setDefaultValues(this, R.xml.preference, false)

        WorkerUtil.periodicWork<SyncManager>(6, TimeUnit.HOURS)
        WorkerUtil.periodicWork<AppUsageStatCollector>(6, TimeUnit.HOURS)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}