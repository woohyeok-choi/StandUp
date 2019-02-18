package kr.ac.kaist.iclab.standup.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.entity.AppUsageStats

class AppUsageStatCollector(context : Context, params : WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if(AppUsageStats.checkEnabled(applicationContext)) {
            val box = App.boxStore.boxFor<AppUsageStats>()
            AppUsageStats.query(box, applicationContext)
        }
        return Result.success()
    }
}