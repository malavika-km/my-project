package com.resquex.app

import com.resquex.app.nlp.EmergencyNlpProcessor
import com.resquex.app.nlp.UrgencyLevel
import com.resquex.app.nlp.VictimType
import com.resquex.app.safety.EmergencyRouter
import com.resquex.app.safety.SafetyCheckResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying on-device NLP processing:
 * Levenshtein distance, typo tolerance, entity extraction, and urgency classification.
 */
class EmergencyNlpProcessorTest {

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, EmergencyNlpProcessor.levenshteinDistance("cpr", "cpr"))
        assertEquals(1, EmergencyNlpProcessor.levenshteinDistance("choking", "chokng"))
        assertEquals(1, EmergencyNlpProcessor.levenshteinDistance("bleed", "blead"))
        assertEquals(1, EmergencyNlpProcessor.levenshteinDistance("burn", "bern"))
        assertEquals(2, EmergencyNlpProcessor.levenshteinDistance("burn", "borns"))
    }

    @Test
    fun testFuzzyMatchTolerance() {
        assertTrue(EmergencyNlpProcessor.isFuzzyMatch("chokng", "choking", 1))
        assertTrue(EmergencyNlpProcessor.isFuzzyMatch("blead", "bleed", 1))
        assertFalse(EmergencyNlpProcessor.isFuzzyMatch("automobile", "bleeding", 2))
    }

    @Test
    fun testVictimDemographicExtraction() {
        val babyAnalysis = EmergencyNlpProcessor.analyze("My baby is suffocating")
        assertEquals(VictimType.INFANT, babyAnalysis.victimType)

        val childAnalysis = EmergencyNlpProcessor.analyze("A 5 year old kid fell and hit his head")
        assertEquals(VictimType.CHILD, childAnalysis.victimType)

        val adultAnalysis = EmergencyNlpProcessor.analyze("My elderly grandmother collapsed")
        assertEquals(VictimType.ADULT, adultAnalysis.victimType)

        val unknownAnalysis = EmergencyNlpProcessor.analyze("Someone has a cut on their finger")
        assertEquals(VictimType.UNKNOWN, unknownAnalysis.victimType)
    }

    @Test
    fun testAnatomicalEntityExtraction() {
        val analysis = EmergencyNlpProcessor.analyze("Deep bleeding laceration on right knee and head")
        assertTrue(analysis.detectedBodyParts.contains("leg")) // knee maps to leg
        assertTrue(analysis.detectedBodyParts.contains("head"))
    }

    @Test
    fun testUrgencyLevelClassification() {
        val critical = EmergencyNlpProcessor.analyze("Hurry please he is unresponsive and not breathing!")
        assertEquals(UrgencyLevel.CRITICAL, critical.urgencyLevel)

        val urgent = EmergencyNlpProcessor.analyze("How to treat a second degree burn on arm?")
        assertEquals(UrgencyLevel.URGENT, urgent.urgencyLevel)

        val routine = EmergencyNlpProcessor.analyze("What supplies should I keep in my first aid kit?")
        assertEquals(UrgencyLevel.ROUTINE, routine.urgencyLevel)
    }

    @Test
    fun testEmergencyRouterCatchesPanicTyposViaNlp() {
        // "chokng" typo should still trigger Choking emergency card!
        val result = EmergencyRouter.evaluate("he is chokng on food cannot breathe")
        assertTrue("Expected CriticalEmergency for typo query", result is SafetyCheckResult.CriticalEmergency)
        val card = (result as SafetyCheckResult.CriticalEmergency).card
        assertEquals("Choking", card.category)
    }
}
