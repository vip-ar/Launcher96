package com.tv96.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLock = true
    private val idleMs = 3 * 60 * 1000L
    private val idleRunnable = Runnable { showLock() }

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 1000)
        }
    }
    private val weatherTask = object : Runnable {
        override fun run() {
            Weather.fetch { t ->
                if (t != null) {
                    temp = t
                    weatherText.text = "$t° عمّان"
                }
            }
            handler.postDelayed(this, 30 * 60 * 1000L)
        }
    }

    private lateinit var wall: WallpaperRotator
    private lateinit var lockLayer: View
    private lateinit var homeLayer: View
    private lateinit var scrim: View
    private lateinit var appsView: RecyclerView
    private val stripViews = ArrayList<View>()
    private var temp: Int? = null

    private fun tv(id: Int) = findViewById<TextView>(id)
    private val clockText by lazy { tv(R.id.clockText) }
    private val ampmText by lazy { tv(R.id.ampmText) }
    private val hijriDate by lazy { tv(R.id.hijriDate) }
    private val gregDate by lazy { tv(R.id.gregDate) }
    private val weatherText by lazy { tv(R.id.weatherText) }
    private val npLabel by lazy { tv(R.id.npLabel) }
    private val npName by lazy { tv(R.id.npName) }
    private val npTime by lazy { tv(R.id.npTime) }
    private val npCount by lazy { tv(R.id.npCount) }
    private val npWords by lazy { tv(R.id.npWords) }
    private val monthName by lazy { tv(R.id.monthName) }
    private val ramLabel by lazy { tv(R.id.ramLabel) }
    private val ramDaysText by lazy { tv(R.id.ramDays) }
    private val ramDateText by lazy { tv(R.id.ramDate) }
    private val homeTime by lazy { tv(R.id.homeTime) }
    private val homeDate by lazy { tv(R.id.homeDate) }
    private val homeNext by lazy { tv(R.id.homeNext) }

    // مخزن اليوم
    private var cachedDate: LocalDate? = null
    private lateinit var today: PrayerTimes.Day
    private var tomorrowFajr = 0L
    private var hijriLine = ""
    private var hijriMonthLine = ""
    private var gregLine = ""
    private var hijriMonthNum = 0
    private var hijriDay = 0
    private var ramDays = -1
    private var ramDateLine = ""
    private var ramYear = 0

    private val prayerNames = arrayOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lockLayer = findViewById(R.id.lockLayer)
        homeLayer = findViewById(R.id.homeLayer)
        scrim = findViewById(R.id.scrim)
        appsView = findViewById(R.id.appsView)
        wall = WallpaperRotator(this, findViewById(R.id.wallA), findViewById(R.id.wallB))

        // الصلوات الخمس
        val strip = findViewById<LinearLayout>(R.id.strip)
        val inflater = LayoutInflater.from(this)
        for (i in 0 until 5) {
            val v = inflater.inflate(R.layout.item_prayer, strip, false)
            v.findViewById<TextView>(R.id.pName).text = prayerNames[i]
            strip.addView(v)
            stripViews.add(v)
        }

        appsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        lockLayer.setOnClickListener { showHome() }
    }

    override fun onResume() {
        super.onResume()
        wall.start()
        loadApps()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        handler.removeCallbacks(weatherTask)
        handler.post(weatherTask)
        resetIdle()
    }

    override fun onPause() {
        super.onPause()
        wall.stop()
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(weatherTask)
        handler.removeCallbacks(idleRunnable)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // زر Home: افتح الرئيسية
        showHome()
        appsView.scrollToPosition(0)
        focusFirstApp()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    private var swallowUpCode = -1

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val code = e.keyCode
        if (e.action == KeyEvent.ACTION_UP && code == swallowUpCode) {
            swallowUpCode = -1
            return true
        }
        val isVolume = code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN ||
                code == KeyEvent.KEYCODE_VOLUME_MUTE
        if (e.action == KeyEvent.ACTION_DOWN && !isVolume) {
            resetIdle()
            if (isLock) {
                if (code != KeyEvent.KEYCODE_BACK) {
                    swallowUpCode = code
                    showHome()
                }
                return true
            }
            if (code == KeyEvent.KEYCODE_BACK) {
                showLock()
                return true
            }
        }
        if (isLock && !isVolume) return true
        return super.dispatchKeyEvent(e)
    }

    // ---------------- الشاشات ----------------

    private fun showHome() {
        if (!isLock) return
        isLock = false
        scrim.setBackgroundResource(R.drawable.scrim_home)
        homeLayer.visibility = View.VISIBLE
        homeLayer.animate().alpha(1f).setDuration(350).start()
        lockLayer.animate().alpha(0f).setDuration(350).withEndAction {
            if (!isLock) lockLayer.visibility = View.GONE
        }.start()
        focusFirstApp()
        resetIdle()
    }

    private fun showLock() {
        if (isLock) return
        isLock = true
        scrim.setBackgroundResource(R.drawable.scrim_lock)
        lockLayer.visibility = View.VISIBLE
        lockLayer.animate().alpha(1f).setDuration(350).start()
        homeLayer.animate().alpha(0f).setDuration(350).withEndAction {
            if (isLock) homeLayer.visibility = View.GONE
        }.start()
        handler.removeCallbacks(idleRunnable)
    }

    private fun resetIdle() {
        handler.removeCallbacks(idleRunnable)
        if (!isLock) handler.postDelayed(idleRunnable, idleMs)
    }

    private fun focusFirstApp() {
        appsView.post { appsView.getChildAt(0)?.requestFocus() }
    }

    // ---------------- التطبيقات ----------------

    private class AppItem(val label: String, val pkg: String, val cls: String, val icon: Drawable)

    private fun loadApps() {
        val pm = packageManager
        val seen = HashSet<String>()
        val list = ArrayList<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                list.add(AppItem(ri.loadLabel(pm).toString(), pkg, ri.activityInfo.name, ri.loadIcon(pm)))
            }
        }
        list.sortBy { it.label.lowercase() }
        appsView.adapter = AppAdapter(list) { app ->
            val i = Intent(Intent.ACTION_MAIN)
            i.component = ComponentName(app.pkg, app.cls)
            i.addCategory(Intent.CATEGORY_LAUNCHER)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                startActivity(i)
            } catch (e: Exception) {
                // التطبيق ما بيفتح: نتجاهل
            }
        }
        if (!isLock) focusFirstApp()
    }

    private inner class AppAdapter(
        private val items: List<AppItem>,
        private val onClick: (AppItem) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.appIcon)
            val label: TextView = v.findViewById(R.id.appLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            v.setOnFocusChangeListener { view, focused ->
                val s = if (focused) 1.12f else 1f
                view.animate().scaleX(s).scaleY(s).setDuration(150).start()
                view.elevation = if (focused) 12f else 0f
            }
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val item = items[position]
            h.icon.setImageDrawable(item.icon)
            h.label.text = item.label
            h.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }

    // ---------------- الوقت والصلاة ----------------

    private fun rebuildDay(date: LocalDate) {
        cachedDate = date
        val h = Hijri.of(date)
        val mName = Hijri.monthName(h)
        val y = Hijri.year(h)
        val d = Hijri.dayOfMonth(h)
        hijriLine = "${Hijri.weekday(date)}، $d $mName $y هـ"
        hijriMonthLine = "$mName $y"
        hijriMonthNum = Hijri.monthNumber(h)
        hijriDay = d
        gregLine = Hijri.gregorian(date)
        today = PrayerTimes.compute(date)
        tomorrowFajr = PrayerTimes.compute(date.plusDays(1)).times[0]

        val r = if (hijriMonthNum == 9) null else Hijri.daysToRamadan(date)
        ramDays = r?.first ?: -1
        ramDateLine = r?.second?.let { "يبدأ تقريباً ${Hijri.weekday(it)} ${it.dayOfMonth} ${Hijri.gMonth(it)}" } ?: ""
        ramYear = r?.second?.let { Hijri.year(Hijri.of(it)) } ?: 0
    }

    private fun fmtShort(ms: Long): String {
        val z = Instant.ofEpochMilli(ms).atZone(PrayerTimes.ZONE)
        val h = if (z.hour % 12 == 0) 12 else z.hour % 12
        return String.format(Locale.ENGLISH, "%d:%02d", h, z.minute)
    }

    private fun fmt(ms: Long): String {
        val z = Instant.ofEpochMilli(ms).atZone(PrayerTimes.ZONE)
        return fmtShort(ms) + " " + (if (z.hour >= 12) "م" else "ص")
    }

    private fun hms(ms: Long): String {
        val s = maxOf(0L, ms / 1000)
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    }

    private fun words(ms: Long): String {
        val m = maxOf(0L, ms / 60000)
        val h = m / 60
        val mm = m % 60
        return when {
            h > 0 && mm > 0 -> "بعد $h ساعة و $mm دقيقة"
            h > 0 -> "بعد $h ساعة"
            else -> "بعد $mm دقيقة"
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val zdt = Instant.ofEpochMilli(now).atZone(PrayerTimes.ZONE)
        val date = zdt.toLocalDate()
        if (date != cachedDate) rebuildDay(date)

        val clock = fmtShort(now)
        val ap = if (zdt.hour >= 12) "م" else "ص"
        clockText.text = clock
        ampmText.text = ap
        homeTime.text = "$clock $ap"
        hijriDate.text = hijriLine
        gregDate.text = gregLine
        homeDate.text = "${Hijri.weekday(date)} · $gregLine · $hijriMonthLine"
        monthName.text = hijriMonthLine

        // الصلاة القادمة
        val l = today.times
        var idx = -1
        for (i in 0 until 5) {
            if (l[i] > now) {
                idx = i
                break
            }
        }
        var tomorrow = false
        val target: Long
        if (idx == -1) {
            idx = 0
            target = tomorrowFajr
            tomorrow = true
        } else {
            target = l[idx]
        }

        for (i in 0 until 5) {
            val v = stripViews[i]
            val isNext = i == idx
            v.findViewById<TextView>(R.id.pTime).text = fmtShort(l[i])
            v.setBackgroundResource(if (isNext) R.drawable.strip_next else 0)
            val color = if (isNext) 0xFF20170D.toInt() else 0xFFFFFFFF.toInt()
            v.findViewById<TextView>(R.id.pName).setTextColor(color)
            v.findViewById<TextView>(R.id.pTime).setTextColor(color)
            v.alpha = if (!isNext && (tomorrow || i < idx)) 0.55f else 1f
        }

        npLabel.text = if (tomorrow) "الصلاة القادمة (غداً)" else "الصلاة القادمة"
        npName.text = prayerNames[idx]
        npTime.text = fmt(target)
        npCount.text = hms(target - now)
        npWords.text = words(target - now)
        homeNext.text = "${prayerNames[idx]}  ${hms(target - now)}"

        // رمضان
        if (hijriMonthNum == 9) {
            ramLabel.text = "رمضان مبارك"
            ramDaysText.text = "$hijriDay"
            ramDateText.text = "اليوم من رمضان"
        } else if (ramDays > 0) {
            ramLabel.text = "رمضان $ramYear"
            ramDaysText.text = "$ramDays يوم"
            ramDateText.text = ramDateLine
        }
    }
}
