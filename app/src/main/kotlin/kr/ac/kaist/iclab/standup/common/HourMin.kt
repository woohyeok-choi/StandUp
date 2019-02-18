package kr.ac.kaist.iclab.standup.common

import java.util.*
import java.util.concurrent.TimeUnit

data class HourMin(val hour: Int, val minute: Int) : Comparable<HourMin> {
    fun asOffsetMillis() : Long = TimeUnit.HOURS.toMillis(hour.toLong()) +
            TimeUnit.MINUTES.toMillis(minute.toLong())

    override fun compareTo(other: HourMin): Int {
        var cmp = Integer.compare(hour, other.hour)
        if(cmp == 0) {
            cmp = Integer.compare(minute, other.minute)
        }
        return cmp
    }

    fun isDayStart() = hour == 0 && minute == 0

    fun isDayEnd() = hour == 24 && minute == 0

    companion object {
        fun now() : HourMin {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = System.currentTimeMillis()
            }.let {
                HourMin(it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE))
            }
        }

        fun getDayEndMillis(precision: TimeUnit) = precision.convert(1, TimeUnit.DAYS)
    }
}