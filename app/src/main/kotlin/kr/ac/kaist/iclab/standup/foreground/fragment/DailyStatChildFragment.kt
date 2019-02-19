package kr.ac.kaist.iclab.standup.foreground.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.tasks.Tasks
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.child_fragment_daily_stat.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.ConfigManager
import kr.ac.kaist.iclab.standup.common.DateTimes.formatDayAgo
import kr.ac.kaist.iclab.standup.common.DateTimes.formatTimeRange
import kr.ac.kaist.iclab.standup.common.EmptyResultException
import kr.ac.kaist.iclab.standup.common.StandUpException
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import kr.ac.kaist.iclab.standup.foreground.LoadStatus
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DailyStatChildFragment : Fragment() {
    private val status = LoadStatus.new()
    private val result = MutableLiveData<PhysicalActivity.Stat?>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass.simpleName, "onCreateView()")
        return inflater.inflate(R.layout.child_fragment_daily_stat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view.context)
        setupObservers()
        loadStat(view.context)
    }

    private fun setupViews(context: Context) {
        btnBefore.alpha = if(arguments?.getBoolean(ARG_IS_FIRST_ITEM) == true) 0.3F else 1.0F
        btnAfter.alpha = if(arguments?.getBoolean(ARG_IS_LAST_ITEM) == true) 0.3F else 1.0F

        val configManager = ConfigManager.getInstance(context)
        val (dailyFrom, dailyTo) = configManager.interventionDailyTimeRange
        val dayStart = arguments?.getLong(ARG_DAY_START_MILLIS)!!
        val isAllDay = (dailyTo.hour - dailyFrom.hour == 24) && dailyFrom.minute == 0 && dailyTo.minute == 0
        val dayStr = formatDayAgo(context, dayStart, System.currentTimeMillis())
        val rangeStr = if(isAllDay) {
            context.getString(R.string.general_all_day)
        } else {
            formatTimeRange(context, dayStart + dailyFrom.asOffsetMillis(), dayStart + dailyTo.asOffsetMillis())
        }
        val timeStr = "$dayStr ($rangeStr)"

        txtStatTime.text = timeStr
    }

    private fun setupObservers() {
        status.observe(this, Observer {
            if(it?.isLoading() == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
            statContainer.visibility = if(it?.isSucceed() == true) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.isFailed() == true) View.VISIBLE else View.GONE
            txtError.setText(
                (it?.error as? StandUpException)?.resId ?: R.string.msg_error_failed_to_load_data
            )
        })

        result.observe(this, Observer { stat ->
            avgStatContainer.visibility = if(stat != null) View.VISIBLE else View.GONE
            totalStatContainer.visibility = if(stat != null) View.VISIBLE else View.GONE
            txtErrorAvgSedentary.visibility = if(stat == null) View.VISIBLE else View.GONE
            txtErrorTotalSedentary.visibility = if(stat == null) View.VISIBLE else View.GONE
            txtAvgSedentaryTime.text = stat?.avgDurationMillis?.let { TimeUnit.MILLISECONDS.toMinutes(it).toString() } ?: ""
            txtTotalSedentaryTime.text = stat?.totalDurationMillis?.let { TimeUnit.MILLISECONDS.toMinutes(it).toString() } ?: ""
        })
    }

    private fun loadStat(context: Context) {
        val dayStart = arguments?.getLong(ARG_DAY_START_MILLIS)!!
        val configManager = ConfigManager.getInstance(context)
        val (dailyFrom, dailyTo) = configManager.interventionDailyTimeRange

        Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            status.postValue(LoadStatus.loading())

            val from = dayStart + dailyFrom.asOffsetMillis()
            val to = dayStart + dailyTo.asOffsetMillis()
            return@Callable PhysicalActivity.statSedentary(App.boxStore.boxFor(), from, to) ?: throw EmptyResultException()
        }).addOnCompleteListener {
            if(it.isSuccessful) {
                status.postValue(LoadStatus.success())
                result.postValue(it.result)
            }  else {
                status.postValue(LoadStatus.failed(it.exception))
            }
        }
    }


    companion object {
        private val ARG_DAY_START_MILLIS = "${DailyStatChildFragment::class.java.name}.ARG_DAY_START_MILLIS"
        private val ARG_IS_FIRST_ITEM = "${DailyStatChildFragment::class.java.name}.ARG_IS_FIRST_ITEM"
        private val ARG_IS_LAST_ITEM = "${DailyStatChildFragment::class.java.name}.ARG_IS_LAST_ITEM"

        fun newInstance(startMillis: Long, isFirstItem: Boolean, isLastItem: Boolean) : DailyStatChildFragment {
            return Bundle().apply {
                putLong(ARG_DAY_START_MILLIS, startMillis)
                putBoolean(ARG_IS_FIRST_ITEM, isFirstItem)
                putBoolean(ARG_IS_LAST_ITEM, isLastItem)
            }.let { DailyStatChildFragment().apply { arguments = it } }
        }
    }
}