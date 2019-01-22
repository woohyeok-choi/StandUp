package kr.ac.kaist.iclab.standup.background

import androidx.lifecycle.MutableLiveData

class Status private constructor(val state: State, val error: Throwable? = null) {
    enum class State(id: Int) {
        STARTED(1),
        STOPPED(2),
        ERROR(3)
    }

    companion object {
        fun started() = Status(State.STARTED)
        fun stopped() = Status(State.STOPPED)
        fun error(t: Throwable? = null) = Status(State.ERROR, t)

        fun new() = MutableLiveData<Status>().apply { postValue(stopped()) }
    }
}