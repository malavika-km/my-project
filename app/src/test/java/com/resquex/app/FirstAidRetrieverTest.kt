package com.resquex.app

import com.resquex.app.rag.FirstAidKnowledgeBase
import com.resquex.app.rag.FirstAidRetriever
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying offline first-aid knowledge base retrieval and RAG grounding.
 */
class FirstAidRetrieverTest {

    @Test
    fun testKnowledgeBaseIntegrity() {
        assertFalse(FirstAidKnowledgeBase.documents.isEmpty())
        for (doc in FirstAidKnowledgeBase.documents) {
            assertTrue(doc.id.isNotBlank())
            assertTrue(doc.category.isNotBlank())
            assertTrue(doc.title.isNotBlank())
            assertTrue(doc.steps.isNotEmpty())
            assertTrue(doc.sourceName.isNotBlank())
            assertTrue(doc.sourceReference.isNotBlank())
            assertTrue(doc.reviewDate.isNotBlank())
            assertTrue(doc.version >= 1)
        }
    }

    @Test
    fun testRetrievalForBurnInjury() {
        val results = FirstAidRetriever.search("How do I cool and treat a hot water burn?")
        assertFalse("Expected matching documents for burn", results.isEmpty())
        val top = results.first()
        assertEquals("Burns", top.document.category)
        assertTrue(top.document.steps.any { it.contains("20 minutes", ignoreCase = true) })
        assertTrue(top.score > 0.3)
    }

    @Test
    fun testRetrievalSynonymExpansion() {
        // "blood" should match "Management of Severe External Bleeding"
        val results = FirstAidRetriever.search("There is blood coming from a wound")
        assertFalse(results.isEmpty())
        assertEquals("Severe Bleeding", results.first().document.category)

        // "fracture" should match "Musculoskeletal Trauma"
        val fractureResults = FirstAidRetriever.search("Suspected bone fracture in leg")
        assertFalse(fractureResults.isEmpty())
        assertEquals("Musculoskeletal Trauma", fractureResults.first().document.category)
    }

    @Test
    fun testGroundedPromptConstruction() {
        val results = FirstAidRetriever.search("how to treat a burn")
        val prompt = FirstAidRetriever.buildGroundedPrompt("how to treat a burn", results)

        assertTrue(prompt.contains("RESQUE-X", ignoreCase = true))
        assertTrue(prompt.contains("VERIFIED FIRST-AID CONTEXT"))
        assertTrue(prompt.contains("USER QUESTION"))
        assertTrue(prompt.contains(results.first().document.sourceName))
    }

    @Test
    fun testDirectFallbackFormatting() {
        val results = FirstAidRetriever.search("Heimlich maneuver choking")
        val fallback = FirstAidRetriever.formatDirectFallbackResponse("choking", results)

        assertTrue(fallback.contains("VERIFIED FIRST-AID PROTOCOL", ignoreCase = true))
        assertTrue(fallback.contains("ACTION STEPS"))
        assertTrue(fallback.contains("Source:"))
    }

    @Test
    fun testUnrelatedQueryReturnsNoHallucinatedGuidance() {
        val results = FirstAidRetriever.search("How do I replace spark plugs in my car engine?")
        assertTrue("Unrelated non-medical query should return empty or below threshold", results.isEmpty())

        val fallback = FirstAidRetriever.formatDirectFallbackResponse("car engine", results)
        assertTrue(fallback.contains("no specific first-aid document directly matched", ignoreCase = true))
    }
}
