package kr.ac.kaist.iclab.standup.foreground.fragment

import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.objectbox.android.AndroidScheduler
import io.objectbox.kotlin.boxFor
import io.objectbox.reactive.DataSubscription
import kotlinx.android.synthetic.main.fragment_home.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.data.Event
import kr.ac.kaist.iclab.standup.data.Event_
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    lateinit var subscription: DataSubscription

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateSedentaryStat()
    }

    private fun updateSedentaryStat() {
        val now = System.currentTimeMillis()

        val todayStartMillis = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEndMillis = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        subscription = App.boxStore.boxFor<Event>().query()
            .greater(Event_.endLocalTime, todayStartMillis)
            .less(Event_.startLocalTime, todayEndMillis)
            .build()
            .subscribe()
            .on(AndroidScheduler.mainThread())
            .transform { list ->
                val sortedList = list.sortedBy { it.endElapsedTime }
                val totalDuration = sortedList.sumBy { it ->
                    (it.endElapsedTime - it.startElapsedTime).toInt()
                }
                return@transform if(sortedList.last().isUpdated) {
                    totalDuration
                } else {
                    totalDuration + (SystemClock.elapsedRealtime() - sortedList.last().endElapsedTime).toInt()
                }
            }.observer {
                txtSedentaryStats.text = DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(it.toLong()))
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription.cancel()
    }
}