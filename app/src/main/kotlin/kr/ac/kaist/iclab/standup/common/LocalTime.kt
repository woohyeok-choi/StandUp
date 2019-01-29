package kr.ac.kaist.iclab.standup.common

import java.time.DayOfWeek
import java.util.*
import java.util.concurrent.TimeUnit

data class LocalTime(val hour: Int, val minute: Int, val second: Int) : Comparable<LocalTime> {
    fun asTodayLocalTimeMillis(nowMillis: Long = System.currentTimeMillis()) =
        GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
        }.timeInMillis


    fun asOffsetMillis() : Long = TimeUnit.HOURS.toMillis(hour.toLong()) +
            TimeUnit.MINUTES.toMillis(minute.toLong()) +
            TimeUnit.SECONDS.toMillis(second.toLong())

    override fun compareTo(other: LocalTime): Int {
        var cmp = Integer.compare(hour, other.hour)
        if(cmp == 0) {
            cmp = Integer.compare(minute, other.minute)
        }
        if(cmp == 0) {
            cmp = Integer.compare(second, other.second)
        }
        return cmp
    }

    companion object {
        fun now() : LocalTime {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = System.currentTimeMillis()
            }.let {
                LocalTime(it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE), it.get(Calendar.SECOND))
            }
        }
    }
}