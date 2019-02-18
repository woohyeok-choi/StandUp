package kr.ac.kaist.iclab.standup.foreground.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.recyclerview.widget.DefaultItemAnimator
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.fragment_timeline.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.common.Messages
import kr.ac.kaist.iclab.standup.common.StandUpException
import kr.ac.kaist.iclab.standup.entity.EventLog
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity_
import kr.ac.kaist.iclab.standup.foreground.LoadStatus
import kr.ac.kaist.iclab.standup.foreground.RangedEntityDataSource
import kr.ac.kaist.iclab.standup.foreground.adapter.TimelineAdapter
import java.util.concurrent.TimeUnit

class TimelineFragment : Fragment() {
    private lateinit var stickyHeaderTimelineAdapter: TimelineAdapter
    private lateinit var viewModel: TimedRangeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()

        EventLog.new(App.boxStore.boxFor(), "Interaction", "TimelineFragment", mapOf("Started" to true))
    }

    override fun onStop() {
        super.onStop()

        EventLog.new(App.boxStore.boxFor(), "Interaction", "TimelineFragment", mapOf("Started" to false))
    }

    private fun setupViews() {
        stickyHeaderTimelineAdapter = TimelineAdapter()

        recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            adapter = stickyHeaderTimelineAdapter
        }

        viewModel = App.boxStore.boxFor<PhysicalActivity>().let {
                TimedRangeViewModel.getEntityViewModel(this, it)
            }

        viewModel.initialStatus.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it?.isLoading() == true
            recyclerView.visibility = if(it?.isSucceed() == true) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.isFailed() == true) View.VISIBLE else View.GONE
            txtError.setText(
                (it?.error as? StandUpException)?.resId ?: R.string.msg_error_failed_to_load_data
            )
        })

        viewModel.rangeStatus.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it?.isLoading() == true
            if(it?.isFailed() == true) {
                Messages.showSnackBar(
                    container,
                    (it.error as? StandUpException)?.resId ?: R.string.msg_error_failed_to_load_data
                )
            }
            stickyHeaderTimelineAdapter.notifyDataSetChanged()
        })

        viewModel.pagedList.observe(this, Observer { list ->
            list?.let { stickyHeaderTimelineAdapter.submitList(it) }
        })
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener { viewModel.refresh() }
    }

    class TimedRangeViewModel(box: Box<PhysicalActivity>) : ViewModel() {
        private val factory = RangedEntityDataSource.Factory(
            box = box,
            property = PhysicalActivity_.startTimeMillis,
            initialStart = DateTimes.asDayStartMillis(System.currentTimeMillis()),
            stepSize = TimeUnit.DAYS.toMillis(1),
            isOrderDesc = true
        )
        val initialStatus: LiveData<LoadStatus> = Transformations.switchMap(factory.source) { it.initialStatus }
        val rangeStatus: LiveData<LoadStatus> = Transformations.switchMap(factory.source) { it.rangeStatus }
        val pagedList = LivePagedListBuilder(factory, 20).build()

        fun refresh() = factory.source.value?.invalidate()

        companion object {
            fun getEntityViewModel(fragment: Fragment, box: Box<PhysicalActivity>) = ViewModelProviders.of(fragment, object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return TimedRangeViewModel(box) as T
                }
            })[TimedRangeViewModel::class.java]
        }
    }

    companion object {
        fun newInstance() = TimelineFragment()
    }
}