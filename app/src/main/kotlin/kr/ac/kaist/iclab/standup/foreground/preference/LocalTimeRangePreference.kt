package kr.ac.kaist.iclab.standup.foreground.preference

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.appyvet.materialrangebar.RangeBar
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.HourMin
import java.lang.Exception
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LocalTimeRangePreference(context: Context, attributeSet: AttributeSet, defStyleAttr: Int): Preference(context, attributeSet, defStyleAttr), RangeBar.OnRangeBarChangeListener {
    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0)

    private var currentValue: Pair<HourMin, HourMin> = DEFAULT_VALUE
    private var txtRangeStart: AppCompatTextView? = null
    private var txtRangeEnd: AppCompatTextView? = null

    private val tickIntervalMin: Int

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.LocalTimeRangePreference)

        tickIntervalMin = typedArray.getInt(R.styleable.LocalTimeRangePreference_ltrp_tickIntervalMin, 30)

        typedArray.recycle()

        layoutResource = R.layout.pref_local_time_range
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a?.getString(index) ?: hourMinRangeToString(DEFAULT_VALUE)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val value = if(defaultValue is String) {
            defaultValue
        } else {
            getPersistedString(hourMinRangeToString(DEFAULT_VALUE))
        }
        updateValue(value)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        (holder?.findViewById(android.R.id.title) as? AppCompatTextView)?.text = title
        (holder?.findViewById(android.R.id.summary) as? AppCompatTextView)?.text = summary

        txtRangeStart = holder?.findViewById(R.id.rangeStart) as? AppCompatTextView
        txtRangeEnd = holder?.findViewById(R.id.rangeEnd) as? AppCompatTextView

        txtRangeStart?.text = formatHourMin(currentValue.first)
        txtRangeEnd?.text = formatHourMin(currentValue.second)

        (holder?.findViewById(R.id.rangeBar) as? RangeBar)?.apply {
            tickStart = MIN_VALUE
            tickEnd = MAX_VALUE
            setTickInterval(tickIntervalMin.toFloat())
            setPinTextFormatter {
                val hour = (it.toInt() / 60)
                val minute = (it.toInt() - 60 * hour)
                val hourStr = hour.toString().padStart(2, '0')
                val minuteStr = minute.toString().padStart(2, '0')

                return@setPinTextFormatter "$hourStr:$minuteStr"
            }

            currentValue.let {
                setRangePinsByValue(
                    it.first.hour * 60F + it.first.minute,
                    it.second.hour * 60F + it.second.minute
                )
            }

            setOnRangeBarChangeListener(this@LocalTimeRangePreference)
        }
    }

    private fun updateValue(value: String) {
        persistString(value)
        currentValue = stringToLocalTimeRange(value) ?: DEFAULT_VALUE
    }

    override fun onRangeChangeListener(rangeBar: RangeBar?, leftPinIndex: Int, rightPinIndex: Int,
                                       leftPinValue: String?, rightPinValue: String?) {
        val value = if(leftPinValue != null && rightPinValue != null) "$leftPinValue-$rightPinValue" else null
        value?.let {
            updateValue(value)

            txtRangeStart?.text = leftPinValue
            txtRangeEnd?.text = rightPinValue
        }
    }

    class ReadWriteDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Pair<HourMin, HourMin>) :
        ReadWriteProperty<Any?, Pair<HourMin, HourMin>> {

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): Pair<HourMin, HourMin> {
            return sharedPreferences.getString(key, "")?.let { stringToLocalTimeRange(it) } ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Pair<HourMin, HourMin>) {
            sharedPreferences.edit().putString(key, hourMinRangeToString(value)).apply()
        }
    }

    class ReadOnlyDelegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Pair<HourMin, HourMin>) :
        ReadOnlyProperty<Any?, Pair<HourMin, HourMin>> {

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): Pair<HourMin, HourMin> {
            return sharedPreferences.getString(key, "")?.let { stringToLocalTimeRange(it) } ?: default
        }
    }

    companion object {
        val DEFAULT_VALUE = Pair(
            HourMin(9, 0),
            HourMin(21, 0)
        )
        private const val MIN_VALUE = 0F
        private const val MAX_VALUE = 24 * 60F

        fun stringToLocalTimeRange(formatTime: String) : Pair<HourMin, HourMin>? {
            return try {
                val parts = formatTime.split("-")
                val fromStr = parts[0].split(":")
                val toStr = parts[1].split(":")
                val fromLocalTime = HourMin(
                    fromStr[0].toInt(), fromStr[1].toInt()
                )
                val toLocalTime = HourMin(
                    toStr[0].toInt(), toStr[1].toInt()
                )
                Pair(fromLocalTime, toLocalTime)
            } catch (e: Exception) {
                null
            }
        }

        private fun formatHourMin(hourMin: HourMin) = "${hourMin.hour.toString().padStart(2, '0')}:${hourMin.minute.toString().padStart(2, '0')}"

        fun hourMinRangeToString(timeRange: Pair<HourMin, HourMin>) : String {
            return "${formatHourMin(timeRange.first)}-${formatHourMin(timeRange.second)}"
        }
    }
}