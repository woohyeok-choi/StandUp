package kr.ac.kaist.iclab.standup.entity

import android.os.SystemClock
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.Box
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.common.DateTimes.elapsedTimeToLocalTime
import kr.ac.kaist.iclab.standup.common.DateTimes.localTimeToElapsedTime

@Entity
data class PhysicalActivity(
    @Id var id: Long = 0L,
    val eventType: String,
    val startElapsedTimeMillis: Long,
    val startTimeMillis: Long,
    var email: String = "",
    var endElapsedTimeMillis: Long = 0L,
    var endTimeMillis: Long = 0L,
    var isExported: Boolean = false
) {
    fun duration() = if(endElapsedTimeMillis == 0L) SystemClock.elapsedRealtime() - startElapsedTimeMillis else
        endElapsedTimeMillis - startElapsedTimeMillis

    data class Stat(val eventType: String, val totalDurationMillis: Long, val avgDurationMillis: Long)

    companion object {
        const val TYPE_SEDENTARY = "TYPE_SEDENTARY"
        const val TYPE_ACTIVE = "TYPE_ACTIVE"

        fun sedentary(box: Box<PhysicalActivity>, elapsedTime: Long, previousEvent: PhysicalActivity? = null) : PhysicalActivity =
            new(box, TYPE_SEDENTARY, elapsedTime, previousEvent)

        fun active(box: Box<PhysicalActivity>, elapsedTime: Long, previousEvent: PhysicalActivity? = null) : PhysicalActivity =
            new(box, TYPE_ACTIVE, elapsedTime, previousEvent)

        fun latestActivity(box: Box<PhysicalActivity>) : PhysicalActivity? = box.query()
                .equal(PhysicalActivity_.endElapsedTimeMillis, 0L)
                .orderDesc(PhysicalActivity_.startElapsedTimeMillis)
                .build()
                .findFirst()

        fun statSedentary(box: Box<PhysicalActivity>, fromMillis: Long, toMillis: Long) : Stat? =
            stat(box, fromMillis, toMillis, TYPE_SEDENTARY)

        fun statActive(box: Box<PhysicalActivity>, fromMillis: Long, toMillis: Long) : Stat? =
            stat(box, fromMillis, toMillis, TYPE_ACTIVE)

        private fun stat(box: Box<PhysicalActivity>, fromMillis: Long, toMillis: Long, eventType: String) : Stat? {
            if(box.count() == 0L) return null

            val results = box.query()
                .greater(PhysicalActivity_.endTimeMillis, fromMillis)
                .or()
                .less(PhysicalActivity_.startTimeMillis, toMillis)
                .and()
                .equal(PhysicalActivity_.eventType, eventType)
                .build()
                .find()
                .mapNotNull {
                    val adjFrom = Math.max(it.startTimeMillis, fromMillis)
                    val adjTo = Math.min(if(it.endTimeMillis == 0L) System.currentTimeMillis() else it.endTimeMillis, toMillis)
                    val duration = adjTo - adjFrom
                    return@mapNotNull if(duration <= 0) null else duration
                }

            return if(results.isEmpty()) null else Stat(eventType, results.sum(), results.average().toLong())
        }

        private fun new(box: Box<PhysicalActivity>, eventType: String, elapsedTime: Long, previousEvent: PhysicalActivity? = null) : PhysicalActivity {
            previousEvent?.apply {
                endElapsedTimeMillis = elapsedTime
                endTimeMillis = DateTimes.elapsedTimeToLocalTime(endElapsedTimeMillis)
            }?.let {
                box.put(it)
            }

            val userId = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val newEvent = PhysicalActivity(
                email = userId,
                eventType = eventType,
                startElapsedTimeMillis = elapsedTime,
                startTimeMillis = DateTimes.elapsedTimeToLocalTime(elapsedTime)
            )
            box.put(newEvent)
            Log.d(PhysicalActivity::class.java.simpleName, "new(): previousEvent = $previousEvent, newEvent = $newEvent")
            return newEvent
        }
    }
}


