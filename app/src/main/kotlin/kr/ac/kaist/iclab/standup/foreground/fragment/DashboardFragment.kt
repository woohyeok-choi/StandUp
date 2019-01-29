package kr.ac.kaist.iclab.standup.foreground.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.google.android.gms.tasks.Tasks
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.BuildConfig
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.*
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import kr.ac.kaist.iclab.standup.foreground.LoadStatus
import kr.ac.kaist.iclab.standup.foreground.adapter.DayViewPagerAdapter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment() {
    private var sedentaryStatus = LoadStatus.new()
    private var sedentaryStats = MutableLiveData<List<DailyStat>>()

    private var activeStatus = LoadStatus.new()
    private var activeStats = MutableLiveData<List<DailyStat>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view.context)
        setupObservers()

        val box = App.boxStore.boxFor<PhysicalActivity>()
        val now = DateTimes.asDayStartMillis(System.currentTimeMillis())
        val (dailyFrom, dailyTo) = ConfigManager.getInstance(view.context).interventionDailyTimeRange

        loadSedentaryChart(box, now, dailyFrom, dailyTo)
        loadActiveChart(box, now, dailyFrom, dailyTo)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setupViews(context: Context) {
        val from = context.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, 0).firstInstallTime.let {
            DateTimes.asDayStartMillis(it)
        } - TimeUnit.DAYS.toMillis(3)

        val to = System.currentTimeMillis().let {
            DateTimes.asDayStartMillis(it)
        }
        val adapter = DayViewPagerAdapter(childFragmentManager, from, to)
        viewPager.adapter = adapter
        viewPager.currentItem = adapter.count - 1
    }

    private fun setupObservers() {
        sedentaryStatus.observe(this, Observer {
            if(it?.isLoading() == true) {
                progressBarSedentary.show()
            } else {
                progressBarSedentary.hide()
            }
            chartWeeklySedentaryStat.visibility = if(it?.isSucceed() == true) View.VISIBLE else View.GONE
            txtErrorSedentary.visibility = if(it?.isFailed() == true) View.VISIBLE else View.GONE
            txtErrorSedentary.setText(
                (it?.error as? StandUpException)?.resId ?: R.string.msg_error_failed_to_load_data
            )
        })

        sedentaryStats.observe(this, Observer { stats ->
            drawChart(chartWeeklySedentaryStat, stats)
        })

        activeStatus.observe(this, Observer {
            if(it?.isLoading() == true) {
                progressBarActive.show()
            } else {
                progressBarActive.hide()
            }
            chartWeeklyActiveStat.visibility = if(it?.isSucceed() == true) View.VISIBLE else View.GONE
            txtErrorActive.visibility = if(it?.isFailed() == true) View.VISIBLE else View.GONE
            txtErrorActive.setText(
                (it?.error as? StandUpException)?.resId ?: R.string.msg_error_failed_to_load_data
            )
        })

        activeStats.observe(this, Observer { stats ->
            drawChart(chartWeeklyActiveStat, stats)
        })
    }

    private fun drawChart(chart: BarChart, stats: List<DailyStat>) {
        /** General */
        val context = chart.context
        val resources = context.resources
        val now = stats.maxBy { it.anchor }?.anchor ?: DateTimes.asDayStartMillis(System.currentTimeMillis())

        /** Labels */
        val labelUnitMin = context.getString(R.string.unit_minute)
        val labelWeeklyAverage = context.getString(R.string.general_weekly_average)

        val labelGroupDailyAvg= context.getString(R.string.general_daily_average)
        val labelGroupDailyTotal = context.getString(R.string.general_daily_total)

        /** Weekly stats */
        val prevSize = stats.filter { it.index != 7 && it.stat != null}.size

        val weeklySumOfDailyAvg = stats.filter {
            it.index != 7 && it.stat != null
        }.sumByDouble {
            it.stat?.avgDurationMillis?.toDouble() ?: 0.0
        }

        val weeklySumOfDailyTotal = stats.filter {
            it.index != 7 && it.stat != null
        }.sumByDouble {
            it.stat?.totalDurationMillis?.toDouble() ?: 0.0
        }

        val weeklyAvgOfDailyAvg = if(prevSize == 0) null else (weeklySumOfDailyAvg / prevSize).let { TimeUnit.MILLISECONDS.toMinutes(it.toLong()) }
        val weeklyAvgOfDailyTotal = if(prevSize == 0) null else (weeklySumOfDailyTotal / prevSize).let { TimeUnit.MILLISECONDS.toMinutes(it.toLong()) }


        /** Width **/
        val groupSpace = 0.1F
        val barSpace = 0.02F
        val barWidth = 0.43F

        /** Build Bar Dataset */
        val weeklyDataSetOfDailyAvg = stats.sortedBy { it.index }.map {
            Log.d(javaClass.simpleName, "avgStats = ${it.index.toFloat()} - ${TimeUnit.MILLISECONDS.toMinutes(it.stat?.avgDurationMillis ?: 0).toFloat()}")
            BarEntry(it.index.toFloat(), TimeUnit.MILLISECONDS.toMinutes(it.stat?.avgDurationMillis ?: 0).toFloat())
        }.let {entries ->
            BarDataSet(entries, labelGroupDailyAvg).apply {
                color = ResourcesCompat.getColor(resources, R.color.primary, null)
                axisDependency = YAxis.AxisDependency.LEFT
                valueTextSize = 0F
            }
        }

        val weeklyDataSetOfDailyTotal = stats.sortedBy { it.index }.map {
            Log.d(javaClass.simpleName, "totalStats = ${it.index.toFloat()} - ${TimeUnit.MILLISECONDS.toMinutes(it.stat?.totalDurationMillis ?: 0).toFloat()}")
            BarEntry(it.index.toFloat(), TimeUnit.MILLISECONDS.toMinutes(it.stat?.totalDurationMillis ?: 0).toFloat())
        }.let { entries ->
            BarDataSet(entries, labelGroupDailyTotal).apply {
                color = ResourcesCompat.getColor(resources, R.color.secondary, null)
                axisDependency = YAxis.AxisDependency.RIGHT
                valueTextSize = 0F
            }
        }

        val weeklyData = BarData(weeklyDataSetOfDailyAvg, weeklyDataSetOfDailyTotal).apply {
            setBarWidth(barWidth)
        }

        chart.data = weeklyData

        /** Setup chart */
        chart.setDrawGridBackground(false)
        chart.isClickable = false
        chart.isDragEnabled = false
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.description.isEnabled = false

        /** Left axis for daily average */
        chart.axisRight.isGranularityEnabled = false
        chart.axisLeft.axisMinimum = 0F
        chart.axisLeft.axisMaximum = weeklyDataSetOfDailyAvg.yMax + 20F
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.labelCount = 5
        chart.axisLeft.setValueFormatter { value, _ -> "${value.toInt()} $labelUnitMin" }
        weeklyAvgOfDailyAvg?.let {
            chart.axisLeft.addLimitLine(
                LimitLine(it.toFloat(), labelWeeklyAverage).apply {
                    lineColor = ResourcesCompat.getColor(resources, R.color.primary, null)
                    labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
                    textSize = 14F
                    lineWidth = 2.0F
                    enableDashedLine(12.0F, 6.0F, 0F)
                }
            )
        }


        /** Right axis for daily total */
        chart.axisRight.isGranularityEnabled = false
        chart.axisRight.axisMinimum = 0F
        chart.axisRight.axisMaximum = weeklyDataSetOfDailyTotal.yMax + 20F
        chart.axisRight.labelCount = 5
        chart.axisRight.setDrawGridLines(false)
        chart.axisRight.setValueFormatter { value, _ -> "${value.toInt()} $labelUnitMin" }
        weeklyAvgOfDailyTotal?.let {
            chart.axisRight.addLimitLine(
                LimitLine(it.toFloat(), labelWeeklyAverage).apply {
                    lineColor = ResourcesCompat.getColor(resources, R.color.secondary, null)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    textSize = 14F
                    lineWidth = 2.0F
                    enableDashedLine(12.0F, 6.0F, 0F)
                }
            )
        }

        /** X axis */
        chart.xAxis.axisMinimum = weeklyData.xMin
        chart.xAxis.axisMaximum = weeklyData.xMin + weeklyData.getGroupWidth(groupSpace, barSpace) * 8
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1.0F
        chart.xAxis.setCenterAxisLabels(true)
        chart.xAxis.setValueFormatter { value, _ ->
            val diff = (7 - value).toInt()
            if(diff < 1) {
                DateTimes.formatDayAgo(context, now - diff * TimeUnit.DAYS.toMillis(1), now)
            } else {
                DateTimes.formatCompactDate(now - diff * TimeUnit.DAYS.toMillis(1))
            }
        }

        chart.groupBars(weeklyData.xMin, groupSpace, barSpace)
        chart.invalidate()
    }

    private fun loadSedentaryChart(box: Box<PhysicalActivity>, dayStart: Long, dailyFrom: LocalTime, dailyTo: LocalTime) = Tasks.call(Executors.newSingleThreadExecutor(), Callable {
        sedentaryStatus.postValue(LoadStatus.loading())

        val results = (0..7).map {
            val anchor = dayStart - it * TimeUnit.DAYS.toMillis(1)
            val from = anchor + dailyFrom.asOffsetMillis()
            val to = anchor + dailyTo.asOffsetMillis()
            DailyStat(7 - it, anchor, PhysicalActivity.statSedentary(box, from, to))
        }

        if(results.all { it.stat == null }) throw EmptyResultException()
        return@Callable results
    }).addOnCompleteListener {
        if(it.isSuccessful) {
            sedentaryStatus.postValue(LoadStatus.success())
            sedentaryStats.postValue(it.result)
        } else {
            sedentaryStatus.postValue(LoadStatus.failed(it.exception))
        }
    }

    private fun loadActiveChart(box: Box<PhysicalActivity>, dayStart: Long, dailyFrom: LocalTime, dailyTo: LocalTime) = Tasks.call(Executors.newSingleThreadExecutor(), Callable {
        activeStatus.postValue(LoadStatus.loading())

        val results = (0..7).map {
            val anchor = dayStart - it * TimeUnit.DAYS.toMillis(1)
            val from = anchor + dailyFrom.asOffsetMillis()
            val to = anchor + dailyTo.asOffsetMillis()
            DailyStat(7 - it, anchor, PhysicalActivity.statActive(box, from, to))
        }

        if(results.all { it.stat == null }) throw EmptyResultException()
        return@Callable results
    }).addOnCompleteListener {
        if(it.isSuccessful) {
            activeStatus.postValue(LoadStatus.success())
            activeStats.postValue(it.result)
        } else {
            activeStatus.postValue(LoadStatus.failed(it.exception))
        }
    }

    data class DailyStat(val index: Int, val anchor: Long, val stat: PhysicalActivity.Stat?)

    companion object {
        fun newInstance() = DashboardFragment()
    }
}