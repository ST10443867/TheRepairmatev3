package com.example.therepairmate.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class RepairStatus(val key: String, val label: String, val roadmapIndex: Int) {
    BOOKING_CONFIRMED("booking_confirmed", "Booking confirmed", 0),
    RECEIVED("received", "Received", 1),
    DIAGNOSING("diagnosing", "Diagnosing", 2),
    PARTS_ORDERED("parts_ordered", "Parts ordered", 3),
    REPAIRING("repairing", "Repairing", 4),
    TESTING("testing", "Testing", 5),
    READY_FOR_PICKUP("ready_for_pickup", "Ready for pickup", 6),
    COMPLETED("completed", "Completed", 7),
    CANCELLED("cancelled", "Cancelled", -1);

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
