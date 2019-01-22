package kr.ac.kaist.iclab.standup.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

fun showToast(context: Context, message: CharSequence) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun showToast(context: Context, message: Int) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun showSnackBar(view: View, message: CharSequence, actionText: CharSequence? = null, action: (() -> Unit)? = null) {
    val snackBar = if (actionText != null) {
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction(actionText) {
            action?.invoke()
        }
    } else {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
    }
    snackBar.show()
}

fun showSnackBar(view: View, message: Int, actionText: Int? = null, action: (() -> Unit)? = null) {
    showSnackBar(view, view.context.getString(message), actionText?.let { view.context.getString(it) }, action)
}