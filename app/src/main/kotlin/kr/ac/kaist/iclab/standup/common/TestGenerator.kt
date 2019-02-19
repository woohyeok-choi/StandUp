package kr.ac.kaist.iclab.standup.common

import android.os.SystemClock
import io.objectbox.Box
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object TestGenerator {
   fun generatePhysicalActivities(size: Int, box: Box<PhysicalActivity>) {
       box.removeAll()

       val now = SystemClock.elapsedRealtime()
       val random = Random(System.currentTimeMillis())

       var prevEndTime = now

       val minDuration = TimeUnit.MINUTES.toMillis(5).toInt()
       val maxDuration = TimeUnit.HOURS.toMillis(6).toInt()

       (0..size).map {
           val eventType = if(it % 2 == 0) PhysicalActivity.TYPE_ACTIVE else PhysicalActivity.TYPE_SEDENTARY
           val endTime = if(it == 0) 0 else prevEndTime
           val startTime = endTime - random.nextInt(minDuration, maxDuration)

           prevEndTime = startTime
           PhysicalActivity(
               email = "test",
               eventType = eventType,
               startElapsedTimeMillis = startTime,
               startTimeMillis = DateTimes.elapsedTimeToLocalTime(startTime),
               endElapsedTimeMillis = endTime,
               endTimeMillis = if(endTime == 0L) 0 else DateTimes.elapsedTimeToLocalTime(endTime)
           )
       }.let {
           box.put(it)
       }
    }
}