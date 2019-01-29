package kr.ac.kaist.iclab.standup.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_TRANSITION_UPDATE
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_TRANSITION_UPDATE

class ActivityTransitionCollector private constructor(){
    private val transitionRequest = ActivityTransitionRequest(
        listOf(
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .setActivityType(DetectedActivity.STILL)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .setActivityType(DetectedActivity.STILL)
                .build()
        )
    )

    val status = Status.newLiveData()

    private fun getTransitionClient(context: Context) = ActivityRecognition.getClient(context)

    private fun buildTransitionIntent(context: Context) = Intent(ACTION_TRANSITION_UPDATE)
        .let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_TRANSITION_UPDATE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        Log.d(javaClass.simpleName, "start()")
        val transition = getTransitionClient(context)

        transition.requestActivityTransitionUpdates(
            transitionRequest, buildTransitionIntent(context)
        ).addOnCompleteListener {
            if(it.isSuccessful) {
                status.postValue(Status.started())
            } else {
                status.postValue(Status.error(it.exception))
            }
        }
    }

    fun stop(context: Context) {
        Log.d(javaClass.simpleName, "stop()")
        val transition = getTransitionClient(context)
        transition.removeActivityTransitionUpdates(
            buildTransitionIntent(context)
        ).addOnCompleteListener {
            if(it.isSuccessful) {
                status.postValue(Status.stopped())
            } else {
                status.postValue(Status.error(it.exception))
            }

        }
    }

    companion object {
        private var instance: ActivityTransitionCollector? = null

        fun getInstance(): ActivityTransitionCollector {
            if(instance == null) {
                instance = ActivityTransitionCollector()
            }
            return instance!!
        }
    }

    data class Status(val state: State, val error: Throwable? = null) {
        enum class State(val id: Int) {
            STARTED(1),
            STOPPED(2),
            ERROR(3)
        }

        companion object {
            fun started() = Status(State.STARTED)
            fun stopped() = Status(State.STOPPED)
            fun error(t: Throwable? = null) = Status(State.ERROR, t)

            fun newLiveData() = MutableLiveData<Status>().apply { postValue(stopped()) }
        }
    }
}