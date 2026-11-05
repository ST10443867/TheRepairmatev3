package com.example.therepairmate.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.example.therepairmate.R

enum class RepairStatus(val key: String, val labelRes: Int, val roadmapIndex: Int) {
    BOOKING_CONFIRMED("booking_confirmed", R.string.status_booking_confirmed, 0),
    RECEIVED("received", R.string.status_received, 1),
    DIAGNOSING("diagnosing", R.string.status_diagnosing, 2),
    PARTS_ORDERED("parts_ordered", R.string.status_parts_ordered, 3),
    REPAIRING("repairing", R.string.status_repairing, 4),
    TESTING("testing", R.string.status_testing, 5),
    READY_FOR_PICKUP("ready_for_pickup", R.string.status_ready_for_pickup, 6),
    COMPLETED("completed", R.string.status_completed, 7),
    CANCELLED("cancelled", R.string.status_cancelled, -1);

    fun label(context: Context): String = context.getString(labelRes)
    val label: String get() = key

    companion object {
        val roadmap: List<RepairStatus> = listOf(
            BOOKING_CONFIRMED, RECEIVED, DIAGNOSING, PARTS_ORDERED, REPAIRING, TESTING, READY_FOR_PICKUP
        )

        fun fromKey(key: String?): RepairStatus =
            entries.firstOrNull { it.key == key } ?: BOOKING_CONFIRMED
    }
}

@IgnoreExtraProperties
data class Diagnostic(
    val deviceType: String = "",
    val issueType: String = "",
    val answers: Map<String, String> = emptyMap()
)

@IgnoreExtraProperties
data class ScheduledSlot(
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timezone: String = ""
)

@IgnoreExtraProperties
data class EstimatedCost(
    val minimum: Long = 0,
    val maximum: Long = 0,
    val currency: String = "ZAR"
)

@IgnoreExtraProperties
data class Repair(
    @get:DocumentId var id: String = "",
    var ticketId: String = "",
    var uid: String = "",
    var deviceModel: String = "",
    var deviceType: String = "",
    var issueDescription: String = "",
    var diagnosticData: Diagnostic = Diagnostic(),
    var status: String = RepairStatus.BOOKING_CONFIRMED.key,
    var techAssigned: String? = null,
    var scheduledSlot: ScheduledSlot = ScheduledSlot(),
    var estimatedCost: EstimatedCost = EstimatedCost(),
    var notes: String = "",
    var imageUrl: String = "",
    var dateCreated: Timestamp? = null,
    var updatedAt: Timestamp? = null
) {
    val statusEnum: RepairStatus get() = RepairStatus.fromKey(status)
}
