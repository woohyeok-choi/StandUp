package kr.ac.kaist.iclab.standup.foreground.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_sign_in.*
import kr.ac.kaist.iclab.standup.App
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.Messages.showToast
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_GOOGLE_SIGN_IN
import kr.ac.kaist.iclab.standup.entity.EventLog

class SignInActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        initListeners()
    }

    private fun initListeners() {
        btnSignIn.setOnClickListener {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build()

            val client = GoogleSignIn.getClient(this, signInOptions)
            val signInIntent = client.signInIntent

            startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            val signIn = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = signIn.getResult(ApiException::class.java)!!
                authFirebaseWithGoogleAccount(account)
            } catch (e: ApiException) {
                e.printStackTrace()
                showToast(this, "${getString(R.string.msg_error_google_sign_in)} - 오류 코드 ${e.statusCode}")
            }
        }
    }

    private fun authFirebaseWithGoogleAccount(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val auth = FirebaseAuth.getInstance()
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                EventLog.new(App.boxStore.boxFor(),
                    "Interaction",
                    "Sign in",
                    mapOf("email" to (account.email ?: "")).toString()
                )
                startActivity(MainActivity.newIntent(this))
                finish()
            }.addOnFailureListener {
                it.printStackTrace()
                showToast(this, R.string.msg_error_firebase_auth)
            }
    }
}