package kr.ac.kaist.iclab.standup

import android.os.SystemClock
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.objectbox.kotlin.boxFor
import kr.ac.kaist.iclab.standup.entity.PhysicalActivity
import kr.ac.kaist.iclab.standup.entity.PhysicalActivityEntity_

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("kr.ac.kaist.iclab.standup", appContext.packageName)
    }

    @Test
    fun justEst() {
        val box = App.boxStore.boxFor<PhysicalActivity>()
        box.removeAll()

        val now = SystemClock.elapsedRealtime()
        val hour = TimeUnit.HOURS.toMillis(1)

        val firstEntities = ((now - 2 * hour)..(now - hour) step TimeUnit.MINUTES.toMillis(10)).map {
            PhysicalActivity(
                eventType = 0,
                startElapsedTimeMillis = it,
                endElapsedTimeMillis = it + TimeUnit.MINUTES.toMillis(10)
            )
        }

        val secondEntities = ((now - hour)..(now) step TimeUnit.MINUTES.toMillis(10)).map {
            PhysicalActivity(
                eventType = 0,
                startElapsedTimeMillis = it,
                endElapsedTimeMillis = it + TimeUnit.MINUTES.toMillis(10)
            )
        }

        box.put(firstEntities)
        box.put(secondEntities)

        val query = box.query()
            .greater(PhysicalActivityEntity_.startElapsedTimeMillis, now - 2 * hour).parameterAlias("start")
            .less(PhysicalActivityEntity_.startElapsedTimeMillis, now - hour).parameterAlias("end")
            .build()

        val result1 = query.find()

        val result2 = query.setParameter("start", now - hour).setParameter("end", now).find()

        Log.d(javaClass.simpleName, "entity1 = $firstEntities")
        Log.d(javaClass.simpleName, "result1 = $result1")

        Log.d(javaClass.simpleName, "entity2 = $secondEntities")
        Log.d(javaClass.simpleName, "result2 = $result2")

    }
}
