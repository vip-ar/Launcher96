package com.tv96.launcher

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** حساب فلكي تقريبي لأوقات الصلاة في عمّان (الفجر والعشاء 18 درجة). */
object PrayerTimes {
    const val LAT = 31.95
    const val LNG = 35.93
    val ZONE: ZoneId = ZoneId.of("Asia/Amman")

    /** times: الفجر، الظهر، العصر، المغرب، العشاء (بالميلي ثانية) */
    class Day(val times: LongArray, val sunrise: Long)

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun fix(a: Double, b: Double): Double = a - b * floor(a / b)

    fun compute(date: LocalDate): Day {
        val jd = date.toEpochDay() + 2440588.0
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0, 24.0)
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        var eqt = q / 15.0 - ra
        if (eqt > 12) eqt -= 24
        if (eqt < -12) eqt += 24

        val midnight = date.atStartOfDay(ZONE)
        val offset = ZONE.rules.getOffset(midnight.toInstant()).totalSeconds / 3600.0
        val noon = 12 + offset - LNG / 15.0 - eqt

        fun t(a: Double): Double {
            val x = (-sin(rad(a)) - sin(rad(decl)) * sin(rad(LAT))) / (cos(rad(decl)) * cos(rad(LAT)))
            return deg(acos(x.coerceIn(-1.0, 1.0))) / 15.0
        }

        val asrAngle = -deg(atan(1.0 / (1.0 + tan(rad(abs(LAT - decl))))))
        val xa = (sin(rad(asrAngle)) - sin(rad(decl)) * sin(rad(LAT))) / (cos(rad(decl)) * cos(rad(LAT)))
        val asrT = deg(acos(xa.coerceIn(-1.0, 1.0))) / 15.0

        val base = midnight.toInstant().toEpochMilli()
        fun ep(h: Double): Long = base + (h * 3600000.0).toLong()

        return Day(
            longArrayOf(
                ep(noon - t(18.0)),
                ep(noon),
                ep(noon + asrT),
                ep(noon + t(0.833)),
                ep(noon + t(18.0))
            ),
            ep(noon - t(0.833))
        )
    }
}
