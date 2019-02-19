package kr.ac.kaist.iclab.standup.entity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.Box
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class EventLog(
    @Id var id: Long = 0L,
    var email: String = "",
    val timestamp: Long,
    val tag: String,
    val message: String,
    val params: String,
    var isExported: Boolean = false
) {
    companion object {
        fun new(box: Box<EventLog>, tag: String, message: String, params: String = "") {
            box.put(EventLog(
                email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                timestamp = System.currentTimeMillis(),
                tag = tag,
                message = message,
                params = params
            ))

            Log.d(tag, "$message - $params")
        }
    }
}