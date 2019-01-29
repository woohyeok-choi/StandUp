package kr.ac.kaist.iclab.standup.foreground.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_sign_in.*
import kr.ac.kaist.iclab.standup.R
import kr.ac.kaist.iclab.standup.common.Messages.showSnackBar
import kr.ac.kaist.iclab.standup.common.RequestCodes.REQUEST_CODE_GOOGLE_SIGN_IN

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
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .onSuccessTask{
                    val credential = GoogleAuthProvider.getCredential(it?.idToken, null)
                    val auth = FirebaseAuth.getInstance()
                    auth.signInWithCredential(credential)
                }.addOnSuccessListener {
                    startActivity(MainActivity.newIntent(this))
                    finish()
                }.addOnFailureListener {
                    it.printStackTrace()
                    showSnackBar(layout_sign_in, "${it.javaClass.name} - ${it.localizedMessage}")
                }
        }
    }
}