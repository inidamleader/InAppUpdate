package com.inidamleader.inappupdate.updater

import android.annotation.SuppressLint
import android.content.IntentSender.SendIntentException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.inidamleader.inappupdate.R
import com.inidamleader.inappupdate.ui.dialog.ConfirmationDialogFragment
import java.lang.ref.WeakReference

class GoogleFlexibleUpdater(
    activity: FragmentActivity,
    private val daysForFlexibleUpdate: Int = DEFAULT_DAYS_FOR_FLEXIBLE_UPDATE
) : LifecycleEventObserver {
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

    // DIALOG
    // Used to avoid IllegalStateException for DialogFragments
    // You can refer to this blog for more explanation:
    // https://medium.com/@alvaro.blanco/avoiding-illegalstateexception-for-dialogfragments-6a8f31c4ce73
    private val pendingDialogs = mutableListOf<ConfirmationDialogFragment>()
    private var canShowDialogs = false

    private val installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            @SuppressLint("SwitchIntDef")
            override fun onStateUpdate(state: InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADING -> listener?.publishProgress(
                        state.bytesDownloaded(),
                        state.totalBytesToDownload()
                    )
                    InstallStatus.DOWNLOADED -> showCompleteUpdateConfirmationDialog()
                    InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                }
            }
        }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
            }
            Lifecycle.Event.ON_START -> {
            }
            Lifecycle.Event.ON_RESUME -> {
                canShowDialogs = true
                pendingDialogs.forEach { it.show() }
                pendingDialogs.clear()
            }
            Lifecycle.Event.ON_PAUSE -> canShowDialogs = false
            Lifecycle.Event.ON_STOP -> {
            }
            Lifecycle.Event.ON_DESTROY -> {
                pendingDialogs.clear()
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            }
            Lifecycle.Event.ON_ANY -> {
            }
        }
    }

    fun checkUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isDownloaded = appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED
            val isAvailable =
                (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                        (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= daysForFlexibleUpdate)

            when {
                isDownloaded -> showCompleteUpdateConfirmationDialog()
                isAvailable -> {
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

    private fun showCompleteUpdateConfirmationDialog() {
        nullableActivity?.let { activity ->
            if (!activity.isFinishing) {
                ConfirmationDialogFragment
                    .new(
                        R.string.notification,
                        R.string.restart_to_complete_update,
                        R.drawable.ic_notification
                    ).also {
                        if (canShowDialogs) it.show()
                        else pendingDialogs.add(it)
                    }
            }
        }
    }

    private fun ConfirmationDialogFragment.show() = nullableActivity?.let {
        show(it.supportFragmentManager, (it as Listener).confirmationDialogTag)
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