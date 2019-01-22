package kr.ac.kaist.iclab.standup.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                ContextCompat.startForegroundService(it, SedentaryRecognitionService.newIntent(it))
            }
        }
    }

}