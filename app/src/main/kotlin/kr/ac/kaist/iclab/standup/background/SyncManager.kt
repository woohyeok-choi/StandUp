package kr.ac.kaist.iclab.standup.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SyncManager(context : Context, params : WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        TODO()
    }
}