package kr.ac.kaist.iclab.standup.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Location(
    @Id var id: Long = 0,
    val localTime: Long,
    val elapsedTime: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)