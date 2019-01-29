package kr.ac.kaist.iclab.standup.foreground.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.vipulasri.timelineview.TimelineView
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import kr.ac.kaist.iclab.standup.foreground.RangedEntityDataSource

class TimelineAdapter: PagedListAdapter<RangedEntityDataSource.RangedData<PhysicalActivity>, TimelineAdapter.ItemViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.create(parent, viewType)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        getItem(position)?.let { holder.bindView(it) }

    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val isFirst = item?.isFirstItemInRange == true
        val isLast = item?.isLastItemInRange == true

        return when {
            isFirst && !isLast -> 1
            !isFirst && isLast -> 2
            isFirst && isLast -> 3
            else -> 0
        }
    }

    class ItemViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        init {
            view.findViewById<RelativeLayout>(R.id.headerContainer).apply {
                visibility = if(viewType == 1 || viewType == 3) View.VISIBLE else View.GONE
            }
        }

        private val timelineView = (view.findViewById<TimelineView>(R.id.timelineView)).apply {
            initLine(viewType)
        }

        private val txtHeaderWeekday = view.findViewById<AppCompatTextView>(R.id.txtHeaderWeekday)
        private val txtHeaderDate = view.findViewById<AppCompatTextView>(R.id.txtHeaderDate)
        private val txtHeaderFullDate = view.findViewById<AppCompatTextView>(R.id.txtHeaderFullDate)

        private val txtItemTitle = view.findViewById<AppCompatTextView>(R.id.txtItemTitle)
        private val txtItemDuration = view.findViewById<AppCompatTextView>(R.id.txtItemDuration)
        private val txtItemTime = view.findViewById<AppCompatTextView>(R.id.txtItemTime)


        fun bindView(data : RangedEntityDataSource.RangedData<PhysicalActivity>) {
            val headerTime = data.from

            txtHeaderWeekday.text = DateTimes.formatWeekday(itemView.context, headerTime).toUpperCase()
            txtHeaderDate.text = DateTimes.formatCompactDate(headerTime)
            txtHeaderFullDate.text = DateTimes.formatDayAgo(itemView.context, headerTime, System.currentTimeMillis())

            if(data.entity.eventType == PhysicalActivity.TYPE_SEDENTARY) {
                timelineView.marker = itemView.context.getDrawable(R.drawable.ic_sedentary_round)
                txtItemTitle.text = itemView.context.getString(R.string.item_title_sedentary)
            } else {
                timelineView.marker = itemView.context.getDrawable(R.drawable.ic_standup_round)
                txtItemTitle.text = itemView.context.getString(R.string.item_title_stand_up)
            }

            txtItemDuration.text = DateTimes.formatDuration(itemView.context, data.entity.duration())
            txtItemTime.text = DateTimes.formatTimeRange(itemView.context, data.entity.startTimeMillis, data.entity.endTimeMillis)
        }

        fun clear() {
            itemView.visibility = View.GONE
        }

        companion object {
            fun create(viewGroup: ViewGroup, viewType: Int) =
                ItemViewHolder(
                    LayoutInflater.from(viewGroup.context).inflate(R.layout.item_timeline, viewGroup, false),
                    viewType
                )
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RangedEntityDataSource.RangedData<PhysicalActivity>>() {
            override fun areItemsTheSame(
                oldItem: RangedEntityDataSource.RangedData<PhysicalActivity>,
                newItem: RangedEntityDataSource.RangedData<PhysicalActivity>
            ): Boolean = oldItem.entity.id == newItem.entity.id

            override fun areContentsTheSame(oldItem: RangedEntityDataSource.RangedData<PhysicalActivity>,
                                            newItem: RangedEntityDataSource.RangedData<PhysicalActivity>
            ): Boolean = oldItem.entity == newItem.entity
        }
    }
}