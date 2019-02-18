package kr.ac.kaist.iclab.standup.common

import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_TRANSITION_UPDATE

data class MockActivityTransitionResult(
    var activityType: Int = DetectedActivity.STILL,
    var transitionType: Int = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
    var elapsedRealTime: Long = 0
) {
    companion object {
        private val EXTRA_ACTIVITY_TYPE = "${MockActivityTransitionResult::class.java.name}.EXTRA_ACTIVITY_TYPE"
        private val EXTRA_TRANSITION_TYPE = "${MockActivityTransitionResult::class.java.name}.EXTRA_TRANSITION_TYPE"
        private val EXTRA_ELAPSED_TIME = "${MockActivityTransitionResult::class.java.name}.EXTRA_ELAPSED_TIME"

        fun still(elapsedRealTime: Long) : Intent = Intent(ACTION_TRANSITION_UPDATE)
            .putExtra(EXTRA_ACTIVITY_TYPE, DetectedActivity.STILL)
            .putExtra(EXTRA_TRANSITION_TYPE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .putExtra(EXTRA_ELAPSED_TIME, elapsedRealTime)

        fun move(elapsedRealTime: Long) : Intent = Intent(ACTION_TRANSITION_UPDATE)
            .putExtra(EXTRA_ACTIVITY_TYPE, DetectedActivity.STILL)
            .putExtra(EXTRA_TRANSITION_TYPE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .putExtra(EXTRA_ELAPSED_TIME, elapsedRealTime)

        fun fromIntent(intent: Intent) = MockActivityTransitionResult(
            activityType = intent.getIntExtra(EXTRA_ACTIVITY_TYPE, DetectedActivity.STILL),
            transitionType = intent.getIntExtra(EXTRA_TRANSITION_TYPE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            elapsedRealTime = intent.getLongExtra(EXTRA_ELAPSED_TIME, 0)
        )
    }
}