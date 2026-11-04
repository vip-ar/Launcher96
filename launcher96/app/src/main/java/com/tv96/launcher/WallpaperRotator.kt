package com.tv96.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.util.concurrent.Executors

/** يبدّل الخلفيات من assets/wallpapers كل عشر دقائق مع انتقال ناعم. */
class WallpaperRotator(
    private val ctx: Context,
    private val a: ImageView,
    private val b: ImageView
) {
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val intervalMs = 10 * 60 * 1000L
    private var files: List<String> = emptyList()
    private var index = -1
    private var showingA = true
    private var first = true

    private val tick = object : Runnable {
        override fun run() {
            next()
            handler.postDelayed(this, intervalMs)
        }
    }

    init {
        files = (ctx.assets.list("wallpapers") ?: emptyArray())
            .filter { it.endsWith(".webp") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") }
            .sorted()
        if (files.isNotEmpty()) index = (System.currentTimeMillis() % files.size).toInt() - 1
    }

    fun start() {
        handler.removeCallbacks(tick)
        if (files.isEmpty()) return
        handler.post(tick)
    }

    fun stop() {
        handler.removeCallbacks(tick)
    }

    private fun next() {
        if (files.isEmpty()) return
        index = (index + 1 + files.size) % files.size
        val name = files[index]
        io.execute {
            val bmp = decode(name)
            if (bmp != null) handler.post { show(bmp) }
        }
    }

    private fun decode(name: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1920) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            ctx.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    private fun show(bmp: Bitmap) {
        val target = if (showingA) b else a
        val other = if (showingA) a else b
        target.setImageBitmap(bmp)
        if (first) {
            // أول صورة بدون انتقال
            target.alpha = 1f
            other.alpha = 0f
            first = false
        } else {
            target.animate().alpha(1f).setDuration(1500).start()
            other.animate().alpha(0f).setDuration(1500).start()
        }
        showingA = !showingA
    }
}
