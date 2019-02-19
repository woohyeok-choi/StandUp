package kr.ac.kaist.iclab.standup.background

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.common.DateTimes
import kr.ac.kaist.iclab.standup.common.DateTimes.formatDateTime
import kr.ac.kaist.iclab.standup.entity.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class SyncManager(context : Context, params : WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            dumpData()
            uploadData()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun uploadData() {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val files = File(Environment.getExternalStorageDirectory(), "StandUp").walkTopDown().toList()

        files.forEach {
            if(it.isFile)
                uploadFile(storageRef, it)
        }
    }

    private fun uploadFile(reference: StorageReference, file: File) {
        val childReference = reference.child(file.name.replace("-", "/"))
        val uploadTask = childReference.putFile(Uri.fromFile(file))

        val result = Tasks.await(uploadTask)

        if(result.task.isSuccessful) {
            file.delete()
        } else {
            result.error?.printStackTrace()
        }
    }

    private fun dumpData() {
        with(App.boxStore.boxFor<AppUsageStats>()) {
            val query = query()
                .equal(AppUsageStats_.isExported, false)
                .build()

            val data = query.find().filter { it.email.isNotEmpty() }.groupBy { it.email }

            exportData("appUsage", data,
                "email,packageName,name,startTimeMillis,startTime,endTimeMillis,endTime,lastTimeUsedMillis,lastTimeUsed,totalTimeForegroundMillis"
            ) {
                "${it.email},${it.packageName},${it.name}," +
                        "${it.startTime},${formatDateTime(it.startTime)}," +
                        "${it.endTime},${formatDateTime(it.endTime)}," +
                        "${it.lastTimeTimeUsed},${formatDateTime(it.lastTimeTimeUsed)}," +
                        "${it.totalTimeForeground}"
            }

            query.remove()
        }

        with(App.boxStore.boxFor<EventLog>()) {
            val query = query()
                .equal(EventLog_.isExported, false)
                .build()

            val data = query.find().filter { it.email.isNotEmpty() }.groupBy { it.email }

            exportData("log", data,
                "email,timestamp,time,tag,message,params") {
                "${it.email}$${it.timestamp}$${formatDateTime(it.timestamp)}$${it.tag}$${it.message}$${it.params}"
            }

            query.remove()
        }


        with(App.boxStore.boxFor<PhysicalActivity>()) {
            val query = query()
                .equal(PhysicalActivity_.isExported, false)
                .notEqual(PhysicalActivity_.endElapsedTimeMillis, 0)
                .build()

            val data = query.find()
            val dataGrouped = data.filter { it.email.isNotEmpty() }.groupBy { it.email }

            exportData("activity", dataGrouped,
                "email,eventType,startElapsedTimeMillis,startTimeMillis,startTime,endElapsedTimeMillis,endTimeMillis,endTime"
            ) {
                "${it.email},${it.eventType}," +
                        "${it.startElapsedTimeMillis},${it.startTimeMillis},${formatDateTime(it.startTimeMillis)}," +
                        "${it.endElapsedTimeMillis},${it.endTimeMillis},${formatDateTime(it.endTimeMillis)}"
            }

            data.forEach { it.isExported = true }
            put(data)
        }
    }

    private inline fun <T> exportData(dataType: String, data: Map<String, List<T>>, header: String? = null, converter: (T) -> String) {
        val fileName = DateTimes.formatDateTime(System.currentTimeMillis())
        val parent = File(Environment.getExternalStorageDirectory(), "StandUp")
        for(key in data.keys) {
            val datum = data[key]
            if(datum?.isEmpty() == true) continue
            parent.mkdirs()

            val file = File(parent, "$key-$dataType-$fileName.csv")
            FileOutputStream(file).bufferedWriter().use { writer ->
                header?.let {
                    writer.write(it)
                    writer.newLine()
                }

                datum?.forEach {
                    writer.write(converter(it))
                    writer.newLine()
                }
            }
        }
    }

    companion object {
        private val TAG = SyncManager::class.java.simpleName
    }
}