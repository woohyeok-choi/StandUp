package kr.ac.kaist.iclab.standup.foreground

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.objectbox.Box
import io.objectbox.Property
import io.objectbox.reactive.DataObserver
import kr.ac.kaist.iclab.standup.common.DateTimes
import java.lang.Exception

class RangedEntityDataSource<T>(box: Box<T>,
                                private val property: Property<T>,
                                private val initialStart: Long,
                                private val stepSize: Long,
                                private val isOrderDesc: Boolean = false) : PageKeyedDataSource<Long, RangedEntityDataSource.RangedData<T>>() {

    data class RangedData<T>(
        val entity: T,
        val from: Long,
        val to: Long,
        val isFirstItemInRange: Boolean = false,
        val isLastItemInRange: Boolean = false
    )

    private val totalQuery = box.query().build()

    private val reuseQuery = if(isOrderDesc) {
        box.query().between(property, 0, 0).orderDesc(property).build()
    } else {
        box.query().between(property, 0, 0).order(property).build()
    }

    private val observer = DataObserver<List<T>> { invalidate() }

    val initialStatus = LoadStatus.new()
    val rangeStatus = LoadStatus.new()

    init {
        totalQuery.subscribe().onlyChanges().weak().observer(observer)
    }

    private fun buildRangedData(fromMillis: Long, toMillis: Long, data: Collection<T>) : List<RangedData<T>> {
        val size = data.size
        return data.mapIndexed { index, entity ->
            RangedData(
                entity = entity,
                from = fromMillis,
                to = toMillis,
                isFirstItemInRange = index == 0,
                isLastItemInRange = index == size - 1
            )
        }
    }

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Long, RangedData<T>>) {
        initialStatus.postValue(LoadStatus.loading())
        try {
            var from = initialStart
            var to = initialStart + stepSize - 1

            var query = reuseQuery.setParameters(property, from, to)
            if(query.count() == 0L) {
                if(isOrderDesc) {
                    to = from
                    from -= (stepSize - 1)
                } else {
                    from = to
                    to += (stepSize - 1)
                }
                query = reuseQuery.setParameters(property, from, to)
            }

            Log.d(javaClass.simpleName, "loadInitial(): $from - $to / ${DateTimes.elapsedTimeToLocalTime(from)} - ${DateTimes.elapsedTimeToLocalTime(to)}")

            val data = query.find()
            val rangedData = buildRangedData(from, to, data)

            if(isOrderDesc) {
                callback.onResult(rangedData, to, from)
            } else {
                callback.onResult(rangedData, from, to)
            }
            initialStatus.postValue(LoadStatus.success())
        } catch (e: Exception) {
            initialStatus.postValue(LoadStatus.failed(e))
            e.printStackTrace()
        }
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Long, RangedData<T>>) {
        rangeStatus.postValue(LoadStatus.loading())

        try {
            val from = if(isOrderDesc) params.key - stepSize else params.key
            val to = (if(isOrderDesc) params.key else params.key + stepSize) - 1

            Log.d(javaClass.simpleName, "loadAfter(): $from - $to / ${DateTimes.elapsedTimeToLocalTime(from)} - ${DateTimes.elapsedTimeToLocalTime(to)}")

            val query = reuseQuery.setParameters(property, from, to)


            val data = query.find()
            val rangedData = buildRangedData(from, to, data)
            if(isOrderDesc) {
                callback.onResult(rangedData, from)
            } else {
                callback.onResult(rangedData, to)
            }
            rangeStatus.postValue(LoadStatus.success())
        } catch (e: Exception) {
            rangeStatus.postValue(LoadStatus.failed(e))
            e.printStackTrace()
        }
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Long, RangedData<T>>) { }

    class Factory<T>(private val box: Box<T>,
                     private val property: Property<T>,
                     private val initialStart: Long,
                     private val stepSize: Long,
                     private val isOrderDesc: Boolean = false
    ) : DataSource.Factory<Long, RangedData<T>>() {
        val source = MutableLiveData<RangedEntityDataSource<T>>()

        override fun create(): DataSource<Long, RangedData<T>> {
            val newSource = RangedEntityDataSource(
                box,
                property,
                initialStart,
                stepSize,
                isOrderDesc
            )
            source.postValue(newSource)
            return newSource
        }
    }
}

