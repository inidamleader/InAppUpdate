package com.inidamleader.inappupdate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.inidamleader.inappupdate.ui.dialog.ConfirmationDialogFragment
import com.inidamleader.inappupdate.updater.GoogleFlexibleUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), GoogleFlexibleUpdater.Listener,
    ConfirmationDialogFragment.Listener {

    private val googleFlexibleUpdater by lazy { GoogleFlexibleUpdater(this) }
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // try catch bloc is used to prevent crash on app, because the code inside is not part of core functionalities,
        // so if there is a problem with GoogleUpdater the app continue running without crash
        try {
            if (savedInstanceState == null && isGooglePlayServicesAvailable())
                googleFlexibleUpdater.also {
                    lifecycle.addObserver(it)
                    it.checkUpdate()
                }
        } catch (e: Exception) {
            // Report this exception
        }

        progressBar = findViewById(R.id.progressBar)

        // This code can be used to test progressBar UI but should be removed on production
        // todo remove this code
        findViewById<Button>(R.id.button).setOnClickListener {
            lifecycle.coroutineScope.launch {
                val max = 154
                for (i in 0..max) {
                    delay(100)
                    onDownloading(i.toLong(), max.toLong())
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
            TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE -> googleFlexibleUpdater.completeUpdate()
            // ...
        }
    }

    override fun onNegativeButtonClick(tag: String) {}

    override val requestCode = REQUEST_CODE_APP_UPDATE

    override val confirmationDialogTag = TAG_FRAGMENT_DIALOG_CONFIRMATION_GOOGLE_UPDATE

    override fun onDownloading(bytesDownloaded: Long, totalBytesToDownload: Long) {
        progressBar.apply {
            if (visibility == INVISIBLE) visibility = VISIBLE
            if (isIndeterminate) isIndeterminate = false
        }
        val max = progressBar.max
        val percentageProgress = ((bytesDownloaded * max) / totalBytesToDownload).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            progressBar.setProgress(percentageProgress, true)
        else progressBar.progress = percentageProgress
    }

    override fun onInstalling() {
        progressBar.isIndeterminate = true
    }

    override fun onPending() {
        progressBar.isIndeterminate = true
    }

    override fun onCanceled() {
        progressBar.visibility = INVISIBLE
    }

    override fun onFailed() {
        progressBar.visibility = INVISIBLE
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