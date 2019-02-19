package kr.ac.kaist.iclab.standup.foreground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_main.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.background.SedentaryRecognitionService
import kr.ac.kaist.iclab.standup.common.Messages
import kr.ac.kaist.iclab.standup.common.Messages.showSnackBar
import kr.ac.kaist.iclab.standup.common.Messages.showToast
import kr.ac.kaist.iclab.standup.common.Permissions
import kr.ac.kaist.iclab.standup.entity.EventLog
import kr.ac.kaist.iclab.standup.foreground.fragment.ConfigFragment
import kr.ac.kaist.iclab.standup.foreground.fragment.DashboardFragment
import kr.ac.kaist.iclab.standup.foreground.fragment.TimelineFragment

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment = when (item.itemId) {
            R.id.menu_bottom_nav_dashboard -> DashboardFragment.newInstance()
            R.id.menu_bottom_nav_timeline -> TimelineFragment.newInstance()
            R.id.menu_bottom_nav_config -> ConfigFragment.newInstance()
            else -> null
        }
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, it)
                .commit()
        }
        supportActionBar?.title = item.title

        return fragment != null
    }

    private var isBackPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolBar)
        navigation.setOnNavigationItemSelectedListener(this)

        Permissions.requestPermission(this) { isAlreadyGranted, isGranted ->
            if(isAlreadyGranted || isGranted) {
                ContextCompat.startForegroundService(this, SedentaryRecognitionService.newIntent(this))
                navigation.selectedItemId = R.id.menu_bottom_nav_dashboard
            }

            if(!isAlreadyGranted) {
                if(isGranted) {
                    Messages.showToast(this, R.string.msg_normal_permission_grated)
                } else {
                    Messages.showToast(this, R.string.msg_error_permission_denied)
                    finish()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventLog.new(App.boxStore.boxFor(), "Interaction", "MainActivity", mapOf("Started" to true).toString())

        Permissions.requestPermission(this) { isAlreadyGranted, isGranted ->
            if(isAlreadyGranted || isGranted) {
               ContextCompat.startForegroundService(this, SedentaryRecognitionService.newIntent(this))
            }

            if(!isAlreadyGranted) {
                showToast(this, if(isGranted) R.string.msg_normal_permission_grated else R.string.msg_error_permission_denied)
            }
        }
    }

    override fun onBackPressed() {
        if(isBackPressedOnce) {
            super.onBackPressed()
        } else {
            isBackPressedOnce = true
            showToast(this, R.string.msg_normal_back_pressed_twice)
            Handler().postDelayed({isBackPressedOnce = false}, 2000)
        }
    }

    override fun onStop() {
        super.onStop()
        EventLog.new(App.boxStore.boxFor(), "Interaction", "MainActivity", mapOf("Started" to false).toString())
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
