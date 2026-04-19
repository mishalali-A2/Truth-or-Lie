package com.futurewatch.truthorlietv

import android.app.Dialog
import android.content.Context
import android.widget.Toast
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.net.Uri
import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment

class InfaticaConsentDialog : AppCompatDialogFragment() {

    companion object {
        private const val PRIVACY_POLICY_URL = "https://infatica-sdk.io/uploads/privacy-policy.pdf"
        private const val TERMS_OF_SERVICE_URL = "https://futurewatch.co/terms"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_CONSENT_SHOWN = "infatica_consent_shown"
        private const val KEY_CONSENT_ACCEPTED = "infatica_consent_accepted"

        fun shouldShowConsent(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasBeenShown = prefs.getBoolean(KEY_CONSENT_SHOWN, false)
            val isAccepted = prefs.getBoolean(KEY_CONSENT_ACCEPTED, false)
            return !hasBeenShown || !isAccepted
        }

        fun hasAccepted(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_CONSENT_ACCEPTED, false)
        }

        fun setConsentAccepted(context: Context, accepted: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_CONSENT_SHOWN, true)
                .putBoolean(KEY_CONSENT_ACCEPTED, accepted)
                .apply()
        }

        fun updateNetworkSdkToggle(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("network_sdk_enabled", enabled)
                .apply()
        }
    }

    private var consentListener: ConsentListener? = null

    interface ConsentListener {
        fun onConsentAccepted()
        fun onConsentDeclined()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        consentListener = parentFragment as? ConsentListener ?: context as? ConsentListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Network Sharing Consent")
            .setMessage(buildConsentMessage())
            .setPositiveButton("Agree") { _, _ ->
                consentListener?.onConsentAccepted()
                setConsentAccepted(requireContext(), true)
                updateNetworkSdkToggle(requireContext(), true)
                InfaticaManager.saveConsent(requireContext(), true)
                InfaticaManager.setEnabled(requireContext(), true)
                dismiss()
            }
            .setNegativeButton("Disagree") { _, _ ->
                consentListener?.onConsentDeclined()
                setConsentAccepted(requireContext(), false)
                updateNetworkSdkToggle(requireContext(), false)
                InfaticaManager.saveConsent(requireContext(), false)
                InfaticaManager.setEnabled(requireContext(), false)
                dismiss()
            }
            .setCancelable(false)
            .create()
    }

    private fun buildConsentMessage(): SpannableString {
        val message = "This app uses network sharing technology. By clicking Agree, you agree to the:\n\n• Privacy Policy\n• Terms of Service\n\nYou can disable this anytime in Settings."

        val spannableString = SpannableString(message)

        //  clickable
        val privacyStart = message.indexOf("Privacy Policy")
        if (privacyStart >= 0) {
            val privacyEnd = privacyStart + "Privacy Policy".length
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUrl(PRIVACY_POLICY_URL, "Privacy Policy")
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        //  clickable
        val termsStart = message.indexOf("Terms of Service")
        if (termsStart >= 0) {
            val termsEnd = termsStart + "Terms of Service".length
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUrl(TERMS_OF_SERVICE_URL, "Terms of Service")
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannableString
    }

    private fun openUrl(url: String, title: String) {
//        try {
//            WebViewDialog(url, title).show(requireContext())
//        } catch (e: Exception) {
//            Toast.makeText(requireContext(), "Could not open $title", Toast.LENGTH_SHORT).show()
//        }
        WebViewDialog(url, title).show(requireContext())
    }

    override fun onStart() {
        super.onStart()
        // Make links clickable in the dialog message
        val messageView = dialog?.findViewById<TextView>(android.R.id.message)
        messageView?.movementMethod = LinkMovementMethod.getInstance()
    }
}