package kr.ac.kaist.iclab.standup.foreground.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.xw.repo.BubbleSeekBar
import kr.ac.kaist.iclab.standup.R

class SeekBarPreference(context: Context, attributeSet: AttributeSet, defStyleAttr: Int): Preference(context, attributeSet, defStyleAttr), BubbleSeekBar.OnProgressChangedListener {
    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0)

    private val tickMin: Int
    private val tickMax: Int
    private val tickCount: Int
    private val tickTextCount: Int

    private var currentValue: Int = 0

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreference)

        tickMin = typedArray.getInt(R.styleable.SeekBarPreference_sbp_min, 0)
        tickMax = typedArray.getInt(R.styleable.SeekBarPreference_sbp_max, 100)
        tickCount = typedArray.getInt(R.styleable.SeekBarPreference_sbp_sectionCount, 10)
        tickTextCount = typedArray.getInt(R.styleable.SeekBarPreference_sbp_sectionTextCount, tickCount)

        typedArray.recycle()

        layoutResource = R.layout.pref_seek_bar
    }


    override fun onSetInitialValue(defaultValue: Any?) {
        val value = if(defaultValue is Int) {
            defaultValue
        } else {
            getPersistedInt(tickMin)
        }
        Log.d(javaClass.simpleName, "onSetInitialValue(defaultValue: $defaultValue): value = ${value}")

        updateValue(value)
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        Log.d(javaClass.simpleName, "onGetDefaultValue(${a?.getInt(index, tickMin)})")
        val default = a?.getInt(index, tickMin) ?: tickMin
        setDefaultValue(default)
        return default
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        Log.d(javaClass.simpleName, "onBindViewHolder()")

        (holder?.findViewById(android.R.id.title) as? TextView)?.text = title
        (holder?.findViewById(android.R.id.summary) as? TextView)?.text = summary

        (holder?.findViewById(R.id.seekBar) as? BubbleSeekBar)?.apply {

            configBuilder
                .seekBySection()
                .showSectionMark()
                .showSectionText()
                .showThumbText()
                .hideBubble()
                .touchToSeek()
                .sectionTextPosition(BubbleSeekBar.TextPosition.BELOW_SECTION_MARK)
                .autoAdjustSectionMark()
                .min(tickMin.toFloat())
                .max(tickMax.toFloat())
                .sectionCount(tickCount)
                .sectionTextInterval(tickCount / tickTextCount)
                .build()

            setProgress(currentValue.toFloat())

            onProgressChangedListener = this@SeekBarPreference
        }
    }

    private fun updateValue(value: Int) {
        persistInt(value)
        currentValue = value
    }

    override fun onProgressChanged(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
        if(fromUser) updateValue(progress)
    }

    override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float) { }

    override fun getProgressOnFinally(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float,
                                      fromUser: Boolean) { }
}
