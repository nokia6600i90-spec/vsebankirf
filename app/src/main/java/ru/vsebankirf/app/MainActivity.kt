package ru.vsebankirf.app

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Единственный экран приложения: полноэкранный WebView с сайтом vsebankirf.ru.
 *
 * Логика переходов по ссылкам:
 *  - ссылки на vsebankirf.ru/www.vsebankirf.ru — открываются внутри приложения;
 *  - переходы на офферы (домены партнёрской сети leads.su и её редиректы, /go/…)
 *    тоже остаются внутри WebView — иначе слетит вся аналитика/постбэки сайта,
 *    построенные на cookie и заголовках именно в этом webview-сеансе;
 *  - tel:, mailto:, а также ссылки на настоящие внешние сайты (не офферы) —
 *    открываются в обычном приложении телефона (звонилка, почта, браузер);
 *  - скачивание файлов (например, справки/оферты в PDF) — через системный
 *    DownloadManager, с уведомлением о ходе загрузки.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineView: View

    private val homeUrl get() = getString(R.string.site_url)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_VseBankirf)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        offlineView = findViewById(R.id.offlineView)

        findViewById<View>(R.id.retryButton).setOnClickListener { loadHomeIfOnline() }
        swipeRefresh.setOnRefreshListener {
            if (isOnline()) webView.reload() else { swipeRefresh.isRefreshing = false; showOffline() }
        }

        setupWebView()

        val targetUrl = intent?.dataString?.takeIf { it.isNotBlank() } ?: homeUrl
        if (isOnline()) webView.loadUrl(targetUrl) else showOffline()
    }

    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.setSupportZoom(true)
        s.builtInZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        // Сайт мобильный и сам под это адаптирован — desktop-режим не нужен.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme ?: ""

                if (scheme == "http" || scheme == "https") {
                    // Остаётся внутри приложения: сам сайт, редиректы на офферы (/go/, /t/)
                    // и переходы на сайты партнёрской сети — всё это часть одного и того же
                    // пользовательского пути и должно проходить через один WebView.
                    return false
                }

                // tel:, mailto:, sms:, intent:// и т.п. — отдаём системе
                try {
                    val open = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(open)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, "Не найдено приложение для этой ссылки", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                hideOffline()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) showOffline()
            }
        }

        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Загрузка началась: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось начать загрузку", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadHomeIfOnline() {
        if (isOnline()) {
            hideOffline()
            webView.loadUrl(homeUrl)
        } else {
            showOffline()
        }
    }

    private fun showOffline() {
        offlineView.visibility = View.VISIBLE
        swipeRefresh.isRefreshing = false
    }

    private fun hideOffline() {
        offlineView.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
