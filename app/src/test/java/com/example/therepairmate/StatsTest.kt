package com.example.therepairmate

import com.example.therepairmate.data.*
import org.junit.Assert.*
import org.junit.Test

class StatsTest {
    private fun r(status: String, tech: String? = null, min: Long = 100, max: Long = 200) =
        Repair(ticketId = "T", status = status, techAssigned = tech, estimatedCost = EstimatedCost(min, max, "ZAR"))

    private val techs = listOf("A", "B", "C")

    @Test fun completedAndCancelledAreNotActive() {
        val s = Stats.compute(listOf(r("received"), r("completed"), r("cancelled"), r("repairing")), techs)
        assertEquals(4, s.total)
        assertEquals(2, s.active)
        assertEquals(1, s.completed)
    }

    @Test fun unassignedCountsOnlyActiveWithoutTech() {
        val s = Stats.compute(
            listOf(r("received"), r("diagnosing", "A"), r("completed"), r("repairing", "  ")), techs
        )
        assertEquals(2, s.unassigned)
    }

    @Test fun workloadListsEveryTechnicianIncludingZero() {
        val s = Stats.compute(listOf(r("repairing", "A"), r("diagnosing", "A"), r("completed", "B")), techs)
        assertEquals(mapOf("A" to 2, "B" to 0, "C" to 0), s.workload)
    }

    @Test fun pipelineSumsActiveEstimatesOnly() {
        val s = Stats.compute(
            listOf(r("received", min = 100, max = 200), r("repairing", min = 50, max = 70), r("completed", min = 999, max = 999)),
            techs
        )
        assertEquals(150L, s.pipelineMin)
        assertEquals(270L, s.pipelineMax)
    }

    @Test fun byStatusCountsEveryStatus() {
        val s = Stats.compute(listOf(r("received"), r("received"), r("ready_for_pickup")), techs)
        assertEquals(2, s.byStatus[RepairStatus.RECEIVED])
        assertEquals(1, s.byStatus[RepairStatus.READY_FOR_PICKUP])
        assertEquals(0, s.byStatus[RepairStatus.CANCELLED])
        assertEquals(1, s.readyForPickup)
    }

    @Test fun emptyListGivesZeros() {
        val s = Stats.compute(emptyList(), techs)
        assertEquals(0, s.total); assertEquals(0, s.active); assertEquals(0L, s.pipelineMax)
    }

    @Test fun stepOfFollowsRoadmap() {
        assertEquals(1, Stats.stepOf(r("received")))
        assertEquals(4, Stats.stepOf(r("repairing")))
        assertEquals(6, Stats.stepOf(r("ready_for_pickup")))
        assertEquals(6, Stats.stepOf(r("completed")))
        assertEquals(0, Stats.stepOf(r("cancelled")))
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", Greeting.forHour(5))
        assertEquals("Good afternoon", Greeting.forHour(12))
        assertEquals("Good afternoon", Greeting.forHour(17))
        assertEquals("Good evening", Greeting.forHour(18))
        assertEquals("Good evening", Greeting.forHour(23))
    }

    @Test fun friendlyErrorFallsBackToMessage() {
        assertEquals("boom", friendlyError(RuntimeException("boom")))
        assertEquals("Something went wrong", friendlyError(RuntimeException()))
    }
}