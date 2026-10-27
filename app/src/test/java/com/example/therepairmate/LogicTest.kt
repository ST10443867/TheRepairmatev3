package com.example.therepairmate

import com.example.therepairmate.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Random

class EstimateServiceTest {
    @Test fun smartphoneScreen() {
        val e = EstimateService.get("Smartphone", "Screen")
        assertEquals(800L, e.min); assertEquals(2500L, e.max); assertEquals("ZAR", e.currency)
    }
    @Test fun desktopIsDiscounted() {
        val e = EstimateService.get("Desktop", "Screen")
        assertEquals(640L, e.min); assertEquals(2000L, e.max)
    }
    @Test fun tabletBattery() {
        val e = EstimateService.get("Tablet", "Battery")
        assertEquals(360L, e.min); assertEquals(810L, e.max)
    }
    @Test fun unknownIssueFallsBackToOther() {
        assertEquals(EstimateService.get("Laptop", "Other"), EstimateService.get("Laptop", "???"))
    }
    @Test fun minNeverExceedsMax() {
        for (d in EstimateService.DEVICES) for (i in EstimateService.ISSUES) {
            val e = EstimateService.get(d, i)
            assertTrue("$d/$i", e.min > 0 && e.min <= e.max)
        }
    }
}

class TicketIdTest {
    @Test fun matchesFormat() {
        repeat(200) { assertTrue(TicketId.PATTERN.matches(TicketId.generate(2026))) }
    }
    @Test fun usesGivenYear() = assertTrue(TicketId.generate(2031).startsWith("QFT-2031-"))
    @Test fun isDeterministicForSeededRandom() =
        assertEquals(TicketId.generate(2026, Random(42)), TicketId.generate(2026, Random(42)))
    @Test fun idsDiffer() = assertNotEquals(TicketId.generate(2026), TicketId.generate(2026))
}

class SlotGeneratorTest {
    private val tuesday = LocalDate.of(2026, 10, 27)
    private fun at(h: Int, m: Int) = ZonedDateTime.of(tuesday, LocalTime.of(h, m), SlotGenerator.ZONE)

    @Test fun weekdayHasEightSlots() =
        assertEquals(8, SlotGenerator.slotsFor(tuesday, at(6, 0), emptySet()).size)

    @Test fun weekendIsClosed() {
        assertTrue(SlotGenerator.slotsFor(LocalDate.of(2026, 10, 31), at(6, 0), emptySet()).isEmpty())
        assertTrue(SlotGenerator.slotsFor(LocalDate.of(2026, 11, 1), at(6, 0), emptySet()).isEmpty())
    }

    @Test fun pastSlotsAreUnavailable() {
        val s = SlotGenerator.slotsFor(tuesday, at(10, 30), emptySet()).associateBy { it.start }
        assertFalse(s.getValue("09:00").available)
        assertFalse(s.getValue("10:00").available)
        assertTrue(s.getValue("11:00").available)
    }

    @Test fun bookedSlotIsUnavailable() {
        val s = SlotGenerator.slotsFor(tuesday, at(6, 0), setOf("2026-10-27_13:00")).associateBy { it.start }
        assertFalse(s.getValue("13:00").available)
        assertTrue(s.getValue("14:00").available)
    }

    @Test fun slotIdFormat() =
        assertEquals("2026-10-27_09:00", SlotGenerator.slotsFor(tuesday, at(6, 0), emptySet()).first().id)
}

class RepairStatusTest {
    @Test fun fromKeyParsesAllValues() =
        RepairStatus.entries.forEach { assertEquals(it, RepairStatus.fromKey(it.key)) }
    @Test fun unknownDefaultsToReceived() = assertEquals(RepairStatus.RECEIVED, RepairStatus.fromKey("nope"))
    @Test fun nullDefaultsToReceived() = assertEquals(RepairStatus.RECEIVED, RepairStatus.fromKey(null))
    @Test fun roadmapHasSixOrderedSteps() {
        assertEquals(6, RepairStatus.roadmap.size)
        assertEquals(RepairStatus.READY_FOR_PICKUP, RepairStatus.roadmap.last())
    }
    @Test fun roadmapIndexes() {
        assertEquals(0, RepairStatus.RECEIVED.roadmapIndex)
        assertEquals(3, RepairStatus.REPAIRING.roadmapIndex)
        assertEquals(6, RepairStatus.COMPLETED.roadmapIndex)
        assertEquals(-1, RepairStatus.CANCELLED.roadmapIndex)
    }
    @Test fun firestoreKeysAreSnakeCase() {
        assertEquals("ready_for_pickup", RepairStatus.READY_FOR_PICKUP.key)
        assertEquals("parts_ordered", RepairStatus.PARTS_ORDERED.key)
    }
}
