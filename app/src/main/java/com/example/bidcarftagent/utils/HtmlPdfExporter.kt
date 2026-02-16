package com.example.bidcarftagent.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object HtmlPdfExporter {
    /**
     * Render HTML into an offscreen WebView, capture as bitmap, and write to a PDF.
     * Returns the absolute path of the written PDF on success, or throws on failure.
     */
    suspend fun createPdfFromHtml(ctx: Context, html: String, outputFile: File): String? =
        suspendCancellableCoroutine { cont ->
            try {
                // WebView must be created and interacted with on the main thread.
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        val webView = WebView(ctx)
                        webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        webView.settings.javaScriptEnabled = true
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                try {
                                    view?.postDelayed({
                                        try {
                                            val scale = ctx.resources.displayMetrics.density
                                            val width = ctx.resources.displayMetrics.widthPixels
                                            val contentHeight = (view?.contentHeight ?: 0)
                                            val heightPx = (contentHeight * scale).toInt().coerceAtLeast(1000)

                                            webView.measure(
                                                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
                                            )
                                            webView.layout(0, 0, width, heightPx)

                                            val bitmap = Bitmap.createBitmap(width, heightPx, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bitmap)
                                            webView.draw(canvas)

                                            val document = PdfDocument()
                                            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                                            val page = document.startPage(pageInfo)
                                            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                            document.finishPage(page)

                                            try {
                                                FileOutputStream(outputFile).use { out ->
                                                    document.writeTo(out)
                                                }
                                                document.close()
                                                webView.destroy()
                                                cont.resume(outputFile.absolutePath)
                                            } catch (e: Exception) {
                                                document.close()
                                                webView.destroy()
                                                cont.resumeWithException(e)
                                            }
                                        } catch (e: Exception) {
                                            webView.destroy()
                                            cont.resumeWithException(e)
                                        }
                                    }, 250)
                                } catch (e: Exception) {
                                    webView.destroy()
                                    cont.resumeWithException(e)
                                }
                            }
                        }
                        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
}

