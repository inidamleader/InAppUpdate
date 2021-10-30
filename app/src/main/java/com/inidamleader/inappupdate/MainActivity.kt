package com.inidamleader.inappupdate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.inidamleader.inappupdate.ui.dialog.ConfirmationDialogFragment
import com.inidamleader.inappupdate.updater.GoogleFlexibleUpdater

class MainActivity : AppCompatActivity(), GoogleFlexibleUpdater.Listener,
    ConfirmationDialogFragment.Listener {

    private val googleFlexibleUpdater by lazy { GoogleFlexibleUpdater(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // try catch bloc is used to prevent crash on app, because the code inside is not part of core functionalities,
        // so if there is a problem with GoogleUpdater the app continue running without crash
        try {
            if (savedInstanceState == null && isGooglePlayServicesAvailable()) {
                googleFlexibleUpdater.also {
                    lifecycle.addObserver(it)
                    it.checkUpdate()
                }
            }
        } catch (e: Exception) {
            // Report this exception
        }

        // This code can be used to test progressBar UI but should be removed on production
//        findViewById<Button>(R.id.button).setOnClickListener {
//            lifecycle.coroutineScope.launch {
//                val max = 154
//                for (i in 0..max) {
//                    delay(100)
//                    publishProgress(i.toLong(), max.toLong())
//                }
//            }
//        }
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
            TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE -> googleFlexibleUpdater.completeUpdate()
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

fun Context.isGooglePlayServicesAvailable() =
    GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS