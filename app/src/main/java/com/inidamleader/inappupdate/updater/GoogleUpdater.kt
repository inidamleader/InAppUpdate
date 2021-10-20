package com.inidamleader.inappupdate.updater

import android.content.IntentSender.SendIntentException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.inidamleader.inappupdate.R
import com.inidamleader.inappupdate.ui.dialog.ConfirmationDialogFragment
import java.lang.ref.WeakReference

class GoogleUpdater(activity: FragmentActivity) : LifecycleObserver {
    init {
        require(activity is Listener)
    }

    private val activityWeakReference = WeakReference(activity)
    private val nullableActivity get() = activityWeakReference.get()
    private val listener get() = nullableActivity as Listener?
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private val installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                when {
                    state.installStatus() == InstallStatus.DOWNLOADING -> listener?.publishProgress(
                        state.bytesDownloaded(),
                        state.totalBytesToDownload()
                    )
                    state.installStatus() == InstallStatus.DOWNLOADED -> showCompleteUpdateDialog()
                    state.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(
                        this
                    )
                }
            }
        }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun checkUpdate() {
        installStateUpdatedListener

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when {
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> showCompleteUpdateDialog()
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        && (appUpdateInfo.clientVersionStalenessDays()
                    ?: -1) >= DAYS_FOR_FLEXIBLE_UPDATE -> {
                    try {
                        nullableActivity?.let { activity ->
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                activity,
                                (activity as Listener).requestCode
                            )
                        }
                    } catch (e: SendIntentException) {
                    }
                }
            }
        }
    }

    private fun showCompleteUpdateDialog() {
        nullableActivity?.let { activity ->
            if (!activity.isFinishing)
                ConfirmationDialogFragment
                    .new(
                        R.string.notification,
                        R.string.restart_to_complete_update,
                        R.drawable.ic_notification
                    )
                    .show(
                        activity.supportFragmentManager,
                        (activity as Listener).confirmationDialogTag
                    )
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    interface Listener {
        val requestCode: Int
        val confirmationDialogTag: String
        fun publishProgress(bytesDownloaded: Long, totalBytesToDownload: Long)
    }

    companion object {
        const val DAYS_FOR_FLEXIBLE_UPDATE = 1
    }
}