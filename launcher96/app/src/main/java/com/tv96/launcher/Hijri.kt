package com.tv96.launcher

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** التاريخ الهجري (أم القرى) وأسماء الأيام والأشهر. */
object Hijri {
    private val MONTHS = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    private val DAYS = arrayOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    private val G_MONTHS = arrayOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"
    )

    fun of(d: LocalDate): HijrahDate = HijrahDate.from(d)
    fun monthNumber(h: HijrahDate): Int = h.get(ChronoField.MONTH_OF_YEAR)
    fun monthName(h: HijrahDate): String = MONTHS[monthNumber(h) - 1]
    fun dayOfMonth(h: HijrahDate): Int = h.get(ChronoField.DAY_OF_MONTH)
    fun year(h: HijrahDate): Int = h.get(ChronoField.YEAR)

    fun weekday(d: LocalDate): String = DAYS[d.dayOfWeek.value - 1]
    fun gMonth(d: LocalDate): String = G_MONTHS[d.monthValue - 1]
    fun gregorian(d: LocalDate): String = "${d.dayOfMonth} ${gMonth(d)} ${d.year}"

    /** عدد الأيام حتى أول رمضان، والتاريخ الميلادي له. */
    fun daysToRamadan(from: LocalDate): Pair<Int, LocalDate>? {
        for (i in 1..400) {
            val x = from.plusDays(i.toLong())
            val h = of(x)
            if (monthNumber(h) == 9 && dayOfMonth(h) == 1) return Pair(i, x)
        }
        return null
    }
}
