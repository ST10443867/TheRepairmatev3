package com.example.therepairmate.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class RepairStatus(val key: String, val label: String, val roadmapIndex: Int) {
    RECEIVED("received", "Received", 0),
    DIAGNOSING("diagnosing", "Diagnosing", 1),
    PARTS_ORDERED("parts_ordered", "Parts ordered", 2),
    REPAIRING("repairing", "Repairing", 3),
    TESTING("testing", "Testing", 4),
    READY_FOR_PICKUP("ready_for_pickup", "Ready for pickup", 5),
    COMPLETED("completed", "Completed", 6),
    CANCELLED("cancelled", "Cancelled", -1);

    companion object {
        val roadmap: List<RepairStatus> = listOf(
            RECEIVED, DIAGNOSING, PARTS_ORDERED, REPAIRING, TESTING, READY_FOR_PICKUP
        )

        fun fromKey(key: String?): RepairStatus =
            entries.firstOrNull { it.key == key } ?: RECEIVED
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
    var status: String = RepairStatus.RECEIVED.key,
    var techAssigned: String? = null,
    var scheduledSlot: ScheduledSlot = ScheduledSlot(),
    var estimatedCost: EstimatedCost = EstimatedCost(),
    var notes: String = "",
    var dateCreated: Timestamp? = null,
    var updatedAt: Timestamp? = null
) {
    val statusEnum: RepairStatus get() = RepairStatus.fromKey(status)
}
