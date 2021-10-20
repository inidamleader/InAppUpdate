package com.inidamleader.inappupdate.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.inidamleader.inappupdate.R

class ConfirmationDialogFragment : DialogFragment() {
    private var listener: Listener? = null

    @StringRes
    private var title = 0

    @StringRes
    private var message = 0

    @DrawableRes
    private var icon = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = requireArguments().getInt(ARG_TITLE)
        message = requireArguments().getInt(ARG_MESSAGE)
        icon = requireArguments().getInt(ARG_ICON)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog =
        AlertDialog.Builder(requireContext()).setTitle(title)
            .setIcon(ContextCompat.getDrawable(requireContext(), icon)
                ?.apply {
                    mutate()
                    alpha = 127
                })
            .setMessage(message)
            .setNegativeButton(R.string.no) { _: DialogInterface?, _: Int ->
                tag?.let { listener?.onNegativeButtonClick(it) }
            }
            .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                tag?.let { listener?.onPositiveButtonClick(it) }
            }
            .create()
            .apply { setCanceledOnTouchOutside(false) }

    interface Listener {
        fun onPositiveButtonClick(tag: String)
        fun onNegativeButtonClick(tag: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (parentFragment != null) parentFragment as Listener else context as Listener
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    companion object {
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_MESSAGE = "ARG_MESSAGE"
        private const val ARG_ICON = "ARG_ICON"

        fun new(
            @StringRes title: Int,
            @StringRes message: Int,
            @DrawableRes icon: Int,
        ): ConfirmationDialogFragment {
            val fragment = ConfirmationDialogFragment()
            val args = Bundle()
            args.putInt(ARG_TITLE, title)
            args.putInt(ARG_MESSAGE, message)
            args.putInt(ARG_ICON, icon)
            fragment.arguments = args
            return fragment
        }
    }
}