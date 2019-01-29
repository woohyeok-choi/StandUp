package kr.ac.kaist.iclab.standup.foreground.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.foreground.fragment.DailyStatChildFragment
import java.util.concurrent.TimeUnit

class DayViewPagerAdapter(fragmentManager: FragmentManager, fromMillis: Long, toMillis: Long) : FragmentStatePagerAdapter(fragmentManager) {
    private val from = DateTimes.asDayStartMillis(fromMillis)
    private val to = DateTimes.asDayStartMillis(toMillis)

    override fun getItem(position: Int): Fragment = DailyStatChildFragment.newInstance(
        from + TimeUnit.DAYS.toMillis(1) * position, position == 0, position == count - 1
    )

    override fun getCount(): Int = TimeUnit.MILLISECONDS.toDays(to - from).toInt() + 1

}