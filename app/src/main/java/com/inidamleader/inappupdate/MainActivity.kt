package com.inidamleader.inappupdate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.inidamleader.inappupdate.ui.dialog.ConfirmationDialogFragment
import com.inidamleader.inappupdate.updater.GoogleUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), GoogleUpdater.Listener,
    ConfirmationDialogFragment.Listener {

    private val googleUpdater by lazy { GoogleUpdater(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // try catch bloc is used to prevent crash on you app,
        // because the code inside it is not part of core functionalities,
        // so if there is a problem with GoogleUpdater the app continue running without crash
        try {
            if (savedInstanceState == null && isGooglePlayServicesAvailable()) {
                googleUpdater.also {
                    it.checkUpdate()
                    lifecycle.addObserver(it)
                }
            }
        } catch (e: Exception) {
            // Report this exception
        }

        // Just for testing UI
        findViewById<Button>(R.id.button).setOnClickListener {
            lifecycle.coroutineScope.launch {
                val max = 154
                for (i in 0..max) {
                    delay(100)
                    publishProgress(i.toLong(), max.toLong())
                }
            }
        }
    }

    // Optional
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e("MY_APP", "Update flow failed! Result code: $resultCode")
                // If the update is cancelled or fails, you can request to start the update again.
            }
        }
    }

    override fun onPositiveButtonClick(tag: String) {
        when (tag) {
            // ...
            TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE -> googleUpdater.completeUpdate()
            // ...
        }
    }

    override fun onNegativeButtonClick(tag: String) {}

    override val requestCode = REQUEST_CODE_APP_UPDATE

    override val confirmationDialogTag = TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE

    override fun publishProgress(bytesDownloaded: Long, totalBytesToDownload: Long) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val max = progressBar.max
        val percentageProgress = ((bytesDownloaded * max) / totalBytesToDownload).toInt()
        if (progressBar.progress != percentageProgress) {
            if (percentageProgress < max) progressBar.visibility = View.VISIBLE
            else progressBar.apply {
                visibility = View.INVISIBLE
                progress = 0
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                progressBar.setProgress(percentageProgress, true)
            else progressBar.progress = percentageProgress
        }
    }

    companion object {
        private const val TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE =
            "TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE"
        private const val REQUEST_CODE_APP_UPDATE = 200
    }
}

fun Context.isGooglePlayServicesAvailable(): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
    return resultCode == ConnectionResult.SUCCESS
}