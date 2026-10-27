package com.resquex.app.rag

import com.resquex.app.model.FirstAidDocument
import com.resquex.app.nlp.EmergencyNlpProcessor
import com.resquex.app.nlp.VictimType

/**
 * Result returned by the retrieval engine with relevance scoring.
 */
data class RetrievalResult(
    val document: FirstAidDocument,
    val score: Double,
    val matchedKeywords: List<String>
)

/**
 * Offline lexical and semantic keyword retrieval engine.
 * Ranks reviewed first-aid documents against user queries and formats
 * grounded prompts for Qwen or zero-AI fallback displays.
 */
object FirstAidRetriever {

    private val STOPWORDS = setOf(
        "a", "an", "the", "and", "or", "is", "are", "was", "were", "to", "for",
        "in", "on", "at", "by", "with", "from", "of", "how", "what", "do", "i",
        "my", "me", "you", "he", "she", "it", "they", "we", "can", "should", "help"
    )

    private val SYNONYMS = mapOf(
        "heart" to listOf("cardiac", "cpr", "chest"),
        "heartattack" to listOf("cardiac", "arrest"),
        "breath" to listOf("breathing", "choke", "airway"),
        "breathe" to listOf("breathing", "respiration"),
        "cut" to listOf("wound", "bleed", "bleeding", "laceration"),
        "blood" to listOf("bleed", "bleeding", "hemorrhage"),
        "fire" to listOf("burn", "scald", "heat"),
        "shock" to listOf("electric", "electrocuted", "voltage"),
        "faint" to listOf("unresponsive", "unconscious", "collapse"),
        "passed" to listOf("unconscious", "unresponsive"),
        "fracture" to listOf("broken", "bone", "sprain"),
        "broken" to listOf("fracture", "bone"),
        "allergic" to listOf("allergy", "anaphylaxis", "epipen"),
        "sting" to listOf("allergy", "anaphylaxis", "bee")
    )

    /**
     * Search the local knowledge base using token overlap, weighted keyword matching,
     * and NLP fuzzy typo tolerance.
     */
    fun search(
        query: String,
        topK: Int = 2,
        minScoreThreshold: Double = 0.20
    ): List<RetrievalResult> {
        val nlpAnalysis = EmergencyNlpProcessor.analyze(query)
        val normalizedTokens = nlpAnalysis.normalizedTokens
        if (normalizedTokens.isEmpty()) return emptyList()

        val expandedTokens = mutableSetOf<String>()
        expandedTokens.addAll(normalizedTokens)
        for (token in normalizedTokens) {
            SYNONYMS[token]?.let { expandedTokens.addAll(it) }
        }

        val results = mutableListOf<RetrievalResult>()

        for (doc in FirstAidKnowledgeBase.documents) {
            var score = 0.0
            val matched = mutableListOf<String>()

            // 1. Category exact or fuzzy match (High weight: +3.0)
            val categoryTokens = tokenize(doc.category)
            if (categoryTokens.any { cat -> expandedTokens.any { t -> t == cat || EmergencyNlpProcessor.isFuzzyMatch(t, cat, 1) } }) {
                score += 3.0
                matched.add(doc.category)
            }

            // 2. Title token matches (+2.0 per match)
            val titleTokens = tokenize(doc.title)
            for (t in titleTokens) {
                if (expandedTokens.any { et -> et == t || EmergencyNlpProcessor.isFuzzyMatch(et, t, 1) }) {
                    score += 2.0
                    matched.add(t)
                }
            }

            // 3. Document keywords matches (+2.5 per match, +1.8 for fuzzy match)
            for (kw in doc.keywords) {
                val kwTokens = tokenize(kw)
                if (kwTokens.any { kt -> expandedTokens.any { et -> et == kt || EmergencyNlpProcessor.isFuzzyMatch(et, kt, 1) } }) {
                    score += 2.5
                    matched.add(kw)
                }
            }

            // 4. Content step terms matches (+0.5 per unique match)
            val stepTokens = doc.steps.flatMap { tokenize(it) }.toSet()
            for (st in stepTokens) {
                if (st in normalizedTokens) {
                    score += 0.5
                }
            }

            // 5. Normalization by query length
            val normalizedScore = score / (normalizedTokens.size * 2.0).coerceAtLeast(1.0)

            if (normalizedScore >= minScoreThreshold) {
                results.add(RetrievalResult(doc, normalizedScore, matched.distinct()))
            }
        }

        return results.sortedByDescending { it.score }.take(topK)
    }

    /**
     * Construct a grounded prompt with retrieved context and NLP triage insights for Qwen SLM inference.
     */
    fun buildGroundedPrompt(userQuery: String, retrievedDocs: List<RetrievalResult>): String {
        val nlpAnalysis = EmergencyNlpProcessor.analyze(userQuery)
        val contextText = if (retrievedDocs.isNotEmpty()) {
            retrievedDocs.joinToString("\n\n---\n\n") { it.document.toGroundedContextString() }
        } else {
            "No specific matching first-aid protocol found in the local knowledge base."
        }

        val demographicNote = when (nlpAnalysis.victimType) {
            VictimType.INFANT -> "\nCRITICAL PATIENT NOTE: Patient appears to be an INFANT/BABY. Apply pediatric resuscitation & choking protocols strictly."
            VictimType.CHILD -> "\nCRITICAL PATIENT NOTE: Patient appears to be a CHILD. Adjust compressions and techniques for pediatric care."
            else -> ""
        }

        return """
You are RESQUE-X, an emergency first-aid AI assistant running on the user's mobile device.
CRITICAL INSTRUCTIONS:
1. Base your answer strictly on the verified first-aid guidance provided below.
2. If the verified guidance does not contain sufficient information to answer safely, explicitly state:
"The verified emergency first-aid knowledge base does not contain sufficient guidance on this topic. Please contact local emergency services (112 / 911) or a healthcare professional immediately."
3. Do NOT invent medical procedures, drug dosages, or unverified treatments.
4. Always prioritize urgent life-saving steps and state applicable warnings.$demographicNote
5. Cite the source name at the end of your guidance.

VERIFIED FIRST-AID CONTEXT:
$contextText

USER QUESTION:
$userQuery

GROUNDED FIRST-AID RESPONSE:
        """.trimIndent()
    }

    /**
     * Formats verified guidance directly for display when no AI model is active.
     */
    fun formatDirectFallbackResponse(query: String, retrievedDocs: List<RetrievalResult>): String {
        if (retrievedDocs.isEmpty()) {
            return """
⚠️ Offline AI is not installed, and no specific first-aid document directly matched your question: "$query".

IMMEDIATE RECOMMENDATION:
• If someone is experiencing a life-threatening medical emergency (unconscious, not breathing, severe bleeding, chest pain, or choking), call 112 or 911 immediately.
• You can browse our verified emergency categories using the 'Emergency Categories' screen in the top bar.
            """.trimIndent()
        }

        val topDoc = retrievedDocs.first().document
        val stepsFormatted = topDoc.steps.mapIndexed { idx, step -> "${idx + 1}. $step" }.joinToString("\n")
        val warningsFormatted = if (topDoc.warnings.isNotEmpty()) {
            "\n\nCRITICAL WARNINGS & CONTRAINDICATIONS:\n" + topDoc.warnings.joinToString("\n") { "• $it" }
        } else ""

        val emergencyWhen = if (topDoc.whenToCallAmbulance.isNotEmpty()) {
            "\n\nWHEN TO CALL 112 / 911 IMMEDIATELY:\n" + topDoc.whenToCallAmbulance.joinToString("\n") { "• $it" }
        } else ""

        return """
[VERIFIED FIRST-AID PROTOCOL: ${topDoc.title.uppercase()}]
(Offline AI model is not installed. Guidance retrieved directly from reviewed local knowledge base)

ACTION STEPS:
$stepsFormatted$warningsFormatted$emergencyWhen

Source: ${topDoc.sourceName} (${topDoc.sourceReference})
Reviewed: ${topDoc.reviewDate} (v${topDoc.version})
        """.trimIndent()
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in STOPWORDS && it.length > 1 }
    }
}
