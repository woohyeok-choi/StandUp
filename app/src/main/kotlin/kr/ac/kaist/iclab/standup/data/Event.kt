package kr.ac.kaist.iclab.standup.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Event(
    @Id var id: Long = 0,
    val startElapsedTime: Long,
    val startLocalTime: Long,
    var endElapsedTime: Long = startElapsedTime,
    var endLocalTime: Long = startLocalTime,
    var isUpdated: Boolean = false
) {
    companion object {
        const val TYPE_INTERVENTION_TRIGGER = 0x0001
        const val TYPE_ENTER_STILL = 0x0002
        const val TYPE_EXIT_STILL = 0x0003
        const val TYPE_SNOOZE = 0x0004
    }
}