package kr.ac.kaist.iclab.standup.foreground.preference

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.*
import com.dpro.widgets.OnWeekdaysChangeListener
import com.dpro.widgets.WeekdaysPicker
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.DayOfWeek
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class DaysOfWeekPreference(context: Context, attrs: AttributeSet, defStyleAttr: Int) : Preference(context, attrs, defStyleAttr), OnWeekdaysChangeListener {
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private var currentValue = DayOfWeek.weekdays()

    init {
        layoutResource = R.layout.pref_days_of_week
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val value = if(defaultValue is Set<*>) {
            defaultValue.map { it.toString() }.toSet()
        } else {
            getPersistedStringSet(DEFAULT_VALUE.map { it.toString() }.toSet())
        }
        updateValue(value)
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a?.getTextArray(index)?.map { it.toString() }?.toSet() ?: setOf<String>()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        (holder?.findViewById(android.R.id.title) as? TextView)?.text = title
        (holder?.findViewById(android.R.id.summary) as? TextView)?.text = summary

        (holder?.findViewById(R.id.dayPicker) as? WeekdaysPicker)?.apply {
            selectedDays = currentValue.map { it.id }
            setOnWeekdaysChangeListener(this@DaysOfWeekPreference)
        }
    }

    override fun onChange(view: View?, clickedDay: Int, selectedDays: MutableList<Int>?) {
        selectedDays?.mapNotNull{ DayOfWeek.fromId(it)?.name }?.let { updateValue(it.toSet()) }
    }

    private fun updateValue(value: Set<String>) {
        persistStringSet(value)
        currentValue = value.map { DayOfWeek.valueOf(it) }.toSet()
    }

    class Delegate(private val sharedPreferences: SharedPreferences, private val key: String, private val default: Set<DayOfWeek>) : ReadWriteProperty<Any?, Set<DayOfWeek>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<DayOfWeek> {
            return sharedPreferences.getStringSet(key, setOf())?.map { DayOfWeek.valueOf(it) }?.toSet() ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<DayOfWeek>) {
            sharedPreferences.edit().putStringSet(key, value.map { it.name }.toSet()).apply()
        }
    }

    companion object {
        val DEFAULT_VALUE = DayOfWeek.weekdays()
    }
}