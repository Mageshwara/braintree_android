package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * The "Learn more" destination for [CreditMessagingView] -- an in-app WebView presented as a
 * bottom sheet.
 *
 * Hand-rolled [Dialog] rather than a `BottomSheetDialogFragment`: no Material Components
 * dependency added for a single dialog. No drag-to-dismiss gesture -- dismissed via the close
 * button or a backdrop tap only.
 */
internal class CreditMessagingLander(context: Context) :
    Dialog(context, R.style.PayPalSavedPaymentMethod_BottomSheetDialog) {

    private val webView: WebView

    init {
        setContentView(R.layout.credit_messaging_lander)
        webView = findViewById(R.id.psp_lander_webview)
        findViewById<ImageView>(R.id.psp_lander_close).setOnClickListener { dismiss() }
        setCanceledOnTouchOutside(true)

        window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    /** Loads [url] and shows the sheet. */
    fun load(url: String) {
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        show()
    }
}
