package kr.ac.kaist.iclab.standup.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_LOCATION_UPDATE
import kr.ac.kaist.iclab.standup.common.Actions.ACTION_TRANSITION_UPDATE
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_LOCATION_UPDATE
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_TRANSITION_UPDATE
import java.util.concurrent.TimeUnit

object ActivityTransitionCollector {
    val status = Status.new()

    private val locationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(TimeUnit.SECONDS.toMillis(10))
        .setSmallestDisplacement(5F)

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

    private fun getLocationClient(context: Context) = LocationServices.getFusedLocationProviderClient(context)

    private fun getTransitionClient(context: Context) = ActivityRecognition.getClient(context)

    private fun buildLocationIntent(context: Context) = Intent(ACTION_LOCATION_UPDATE)
        .let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_LOCATION_UPDATE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    private fun buildTransitionIntent(context: Context) = Intent(ACTION_TRANSITION_UPDATE)
        .let {
            PendingIntent.getBroadcast(context, REQUEST_CODE_TRANSITION_UPDATE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        Log.d(javaClass.simpleName, "start()")
        val location = getLocationClient(context)
        val transition = getTransitionClient(context)

        Tasks.whenAll(
            location.requestLocationUpdates(locationRequest, buildLocationIntent(context)),
            transition.requestActivityTransitionUpdates(transitionRequest, buildTransitionIntent(context))
        ).addOnSuccessListener {
            status.postValue(Status.started())
        }.addOnFailureListener {
            status.postValue(Status.error(it))
        }
    }

    fun stop(context: Context) {
        Log.d(javaClass.simpleName, "stop()")
        val location = getLocationClient(context)
        val transition = getTransitionClient(context)

        Tasks.whenAll(
            location.removeLocationUpdates(buildLocationIntent(context)),
            transition.removeActivityTransitionUpdates(buildTransitionIntent(context))
        ).addOnSuccessListener {
            status.postValue(Status.stopped())
        }.addOnFailureListener {
            status.postValue(Status.error(it))
        }
    }
}