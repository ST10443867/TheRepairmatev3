package com.example.therepairmate.data


import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Random
import kotlin.math.roundToInt

data class Estimate(val min: Long, val max: Long, val currency: String, val duration: String)

/** Preliminary estimates only. Replace the tables with Remote Config / Firestore later. */
object EstimateService {
    val DEVICES = listOf("Laptop", "Desktop", "Smartphone", "Tablet", "Game console", "Other")
    val ISSUES = listOf("Screen", "Battery", "Charging", "Performance", "Software", "Water damage", "Other")
    val PHONE_MODELS = listOf(
        "Apple iPhone 15 Pro Max", "Apple iPhone 15 Pro", "Apple iPhone 15", "Apple iPhone 14 Pro Max",
        "Apple iPhone 14 Pro", "Apple iPhone 14", "Apple iPhone 13", "Apple iPhone 12", "Apple iPhone 11", "Apple iPhone SE",
        "Samsung Galaxy S24 Ultra", "Samsung Galaxy S24+", "Samsung Galaxy S24", "Samsung Galaxy S23 Ultra",
        "Samsung Galaxy S23+", "Samsung Galaxy S23", "Samsung Galaxy S22 Ultra", "Samsung Galaxy S22",
        "Samsung Galaxy A54 5G", "Samsung Galaxy A34 5G", "Samsung Galaxy Z Fold5", "Samsung Galaxy Z Flip5",
        "Huawei P60 Pro", "Huawei Mate 50 Pro", "Huawei P50 Pro", "Huawei Nova 11", "Huawei Y9 Prime",
        "Xiaomi 13 Pro", "Xiaomi 13", "Xiaomi Redmi Note 12 Pro", "Xiaomi POCO X5 Pro",
        "Google Pixel 8 Pro", "Google Pixel 8", "Google Pixel 7 Pro", "Google Pixel 7", "Google Pixel 6a",
        "OnePlus 11", "OnePlus 10 Pro", "OnePlus Nord 3"
    )

    private val base = mapOf(
        "Screen" to Triple(800, 2500, "1–2 days"), "Battery" to Triple(400, 900, "Same day"),
        "Charging" to Triple(350, 800, "1 day"), "Performance" to Triple(300, 700, "1–2 days"),
        "Software" to Triple(250, 600, "Same day"), "Water damage" to Triple(900, 3000, "3–5 days"),
        "Other" to Triple(300, 1500, "2–4 days")
    )
    private val factor = mapOf("Desktop" to 0.8, "Tablet" to 0.9, "Game console" to 0.9)

    fun get(deviceType: String, issueType: String): Estimate {
        val (lo, hi, time) = base[issueType] ?: base.getValue("Other")
        val f = factor[deviceType] ?: 1.0
        fun r(v: Int) = ((v * f) / 10).roundToInt() * 10L
        return Estimate(r(lo), r(hi), "ZAR", time)
    }
}

object TicketId {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val PATTERN = Regex("^QFT-\\d{4}-[A-HJ-NP-Z2-9]{5}$")

    fun generate(year: Int = LocalDate.now().year, random: Random = SecureRandom()): String =
        "QFT-$year-" + (1..5).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
}

data class Slot(val date: String, val start: String, val end: String, val available: Boolean) {
    val id: String get() = "${date}_$start"
}

object SlotGenerator {
    val ZONE: ZoneId = ZoneId.of("Africa/Johannesburg")
    private val hours = (9..16).toList()

    /** Mon–Fri, 09:00–17:00, one-hour slots. Booked or already-started slots are unavailable. */
    fun slotsFor(date: LocalDate, now: ZonedDateTime, bookedIds: Set<String>): List<Slot> {
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return emptyList()
        return hours.map { h ->
            val start = "%02d:00".format(h)
            val end = "%02d:00".format(h + 1)
            val startsAt = ZonedDateTime.of(date, LocalTime.of(h, 0), ZONE)
            Slot(date.toString(), start, end, startsAt.isAfter(now) && "${date}_$start" !in bookedIds)
        }
    }
}