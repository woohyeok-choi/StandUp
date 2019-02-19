package kr.ac.kaist.iclab.standup

import android.app.Application
import io.objectbox.BoxStore
import io.objectbox.DebugFlags
import kr.ac.kaist.iclab.standup.entity.MyObjectBox

class App: Application() {

    companion object {
        lateinit var boxStore: BoxStore
    }

    override fun onCreate() {
        super.onCreate()

        boxStore = MyObjectBox.builder()
            .maxSizeInKByte(1024 * 1024 * 2)
            .androidContext(applicationContext)
            .name("stand-up")
            .build()
    }
}