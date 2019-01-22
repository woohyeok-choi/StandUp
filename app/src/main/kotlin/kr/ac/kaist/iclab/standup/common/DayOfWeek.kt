package kr.ac.kaist.iclab.standup.common

import java.util.*

enum class DayOfWeek(val id: Int) {
    SUNDAY(Calendar.SUNDAY),
    MONDAY(Calendar.MONDAY),
    TUESDAY(Calendar.TUESDAY),
    WEDNESDAY(Calendar.WEDNESDAY),
    THURSDAY(Calendar.THURSDAY),
    FRIDAY(Calendar.FRIDAY),
    SATURDAY(Calendar.SATURDAY);

    fun checkSameDayOfWeek(nowMillis: Long) : Boolean {
        return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = nowMillis
        }.get(Calendar.DAY_OF_WEEK) == id
    }

    companion object {
        private val MAP = values().associate { Pair(it.id, it) }

        fun fromId(id: Int) = MAP[id]

        fun weekdays() = setOf(
            MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
        )

        fun weekend() = setOf(
            SUNDAY, SATURDAY
        )

        fun allDays() = DayOfWeek.values().toSet()

        fun today() : DayOfWeek {
            val id = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = System.currentTimeMillis()
            }.get(Calendar.DAY_OF_WEEK)
            return MAP[id]!!
        }
    }
}