package com.example.therepairmate.data


data class AdminStats(
    val total: Int,
    val active: Int,
    val unassigned: Int,
    val readyForPickup: Int,
    val completed: Int,
    val byStatus: Map<RepairStatus, Int>,
    val workload: Map<String, Int>,
    /** Sum of customer estimates for active repairs. This is NOT revenue: no payment data is stored yet. */
    val pipelineMin: Long,
    val pipelineMax: Long
)

object Stats {
    fun isActive(r: Repair): Boolean =
        r.statusEnum != RepairStatus.COMPLETED && r.statusEnum != RepairStatus.CANCELLED

    /** Current step on the 6-step roadmap: 1..6, completed = 6, cancelled = 0. */
    fun stepOf(r: Repair): Int = minOf(r.statusEnum.roadmapIndex + 1, 6)

    fun compute(repairs: List<Repair>, technicians: List<String>): AdminStats {
        val active = repairs.filter { isActive(it) }
        return AdminStats(
            total = repairs.size,
            active = active.size,
            unassigned = active.count { it.techAssigned.isNullOrBlank() },
            readyForPickup = repairs.count { it.statusEnum == RepairStatus.READY_FOR_PICKUP },
            completed = repairs.count { it.statusEnum == RepairStatus.COMPLETED },
            byStatus = RepairStatus.values().associateWith { s -> repairs.count { it.statusEnum == s } },
            workload = technicians.associateWith { t -> active.count { it.techAssigned == t } },
            pipelineMin = active.sumOf { it.estimatedCost.minimum },
            pipelineMax = active.sumOf { it.estimatedCost.maximum }
        )
    }
}

object Greeting {
    fun forHour(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}