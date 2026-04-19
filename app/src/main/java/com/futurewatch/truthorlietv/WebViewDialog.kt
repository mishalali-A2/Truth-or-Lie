package com.futurewatch.truthorlietv

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class WebViewDialog(private val url: String, private val title: String) {

    companion object {
        private const val WEB_VIEW_ID = 1001
        private const val PROGRESS_BAR_ID = 1002
        private const val CLOSE_BUTTON_ID = 1003
    }

    fun show(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(createWebViewLayout(context))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        val webView = dialog.findViewById<WebView>(WEB_VIEW_ID)
        val progressBar = dialog.findViewById<ProgressBar>(PROGRESS_BAR_ID)
        val closeButton = dialog.findViewById<Button>(CLOSE_BUTTON_ID)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
        }
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = android.view.View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("http://") || it.startsWith("https://")) {
                        view?.loadUrl(it)
                        return true
                    }
                }
                return false
            }
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                android.util.Log.e("WebViewError", "Error: $description")
            }
        }

        val pdfUrl = if (url.endsWith(".pdf")) {
            "https://drive.google.com/viewerng/viewer?embedded=true&url=$url"
        } else {
            url
        }

        webView.loadUrl(pdfUrl)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createWebViewLayout(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)

            // Title bar
            val titleBar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 32, 32, 32)
                setBackgroundColor(Color.parseColor("#1a1a2e"))
            }

            val titleText = TextView(context).apply {
                text = this@WebViewDialog.title
                textSize = 24f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val closeBtn = Button(context).apply {
                text = "✕ Close"
                textSize = 18f
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(Color.parseColor("#FFA500"))
                id = CLOSE_BUTTON_ID
            }

            titleBar.addView(titleText)
            titleBar.addView(closeBtn)


            val progressBar = ProgressBar(context).apply {
                id = PROGRESS_BAR_ID
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                visibility = android.view.View.GONE
            }

            // WebView
            val webView = WebView(context).apply {
                id = WEB_VIEW_ID
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }

            addView(titleBar)
            addView(progressBar)
            addView(webView)
        }
    }
}