package com.inidamleader.inappupdate.updater

import android.annotation.SuppressLint
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

class FlexibleGoogleUpdater(
    activity: FragmentActivity,
    private val daysForFlexibleUpdate: Int = DEFAULT_DAYS_FOR_FLEXIBLE_UPDATE
) : LifecycleObserver {
    init {
        require(activity is Listener)
    }

    // MANAGER
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    // SAFE ACTIVITY REF
    private val activityWeakReference = WeakReference(activity)
    private val nullableActivity get() = activityWeakReference.get()

    // LISTENER
    private val listener get() = nullableActivity as Listener?

    private val installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            @SuppressLint("SwitchIntDef")
            override fun onStateUpdate(state: InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADING -> listener?.publishProgress(
                        state.bytesDownloaded(),
                        state.totalBytesToDownload()
                    )
                    InstallStatus.DOWNLOADED -> showCompleteUpdateDialog()
                    InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                }
            }
        }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    fun checkUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when {
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> showCompleteUpdateDialog()
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        && (appUpdateInfo.clientVersionStalenessDays()
                    ?: -1) >= daysForFlexibleUpdate -> {
                    try {
                        nullableActivity?.let { activity ->
                            if (!activity.isFinishing)
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

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    // The listener that has to be implemented by the activity
    interface Listener {
        val requestCode: Int
        val confirmationDialogTag: String
        fun publishProgress(bytesDownloaded: Long, totalBytesToDownload: Long)
    }

    companion object {
        const val DEFAULT_DAYS_FOR_FLEXIBLE_UPDATE = 1
    }
}