package kr.ac.kaist.iclab.standup.foreground

import androidx.lifecycle.MutableLiveData

data class LoadStatus(val state: State, val error: Throwable? = null) {
    enum class State {
        INIT, LOADING, SUCCESS, FAILED
    }

    fun isLoading() = state == State.LOADING

    fun isSucceed() = state == State.SUCCESS

    fun isFailed() = state == State.FAILED

    companion object {
        fun loading() =
            LoadStatus(State.LOADING)
        fun success() =
            LoadStatus(State.SUCCESS)
        fun failed(e: Throwable?) = LoadStatus(
            State.FAILED,
            e
        )

        fun new() = MutableLiveData<LoadStatus>().apply { postValue(
            LoadStatus(State.INIT)
        ) }
    }
}