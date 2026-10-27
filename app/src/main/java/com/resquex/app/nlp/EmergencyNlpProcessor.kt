package com.resquex.app.nlp

/**
 * Victim demographic detected from natural language queries.
 * Critical because CPR ratios and choking maneuvers differ between adults and infants.
 */
enum class VictimType {
    ADULT,
    CHILD,
    INFANT,
    UNKNOWN
}

/**
 * Urgency classification inferred from linguistic cues.
 */
enum class UrgencyLevel {
    CRITICAL,   // Life-threatening, immediate physical intervention required
    URGENT,     // Needs rapid first aid to prevent deterioration
    ROUTINE     // Informational query, minor injuries, post-incident care
}

/**
 * Structured NLP analysis result extracted from natural language input.
 */
data class NlpAnalysisResult(
    val originalQuery: String,
    val normalizedTokens: List<String>,
    val victimType: VictimType,
    val urgencyLevel: UrgencyLevel,
    val detectedBodyParts: List<String>,
    val detectedSymptoms: List<String>,
    val correctedQuery: String
)

/**
 * Lightweight, fast, on-device NLP processing engine for emergency triage.
 * Operates offline with zero external dependencies.
 *
 * Capabilities:
 * 1. Tokenization & Medical Stopword Filtering
 * 2. Typo-tolerant Fuzzy Matching (Levenshtein Distance with length guards)
 * 3. Victim Entity Extraction (Adult / Child / Infant)
 * 4. Urgency & Distress Level Classification
 * 5. Anatomical Entity Recognition
 */
object EmergencyNlpProcessor {

    private val STOPWORDS = setOf(
        "a", "an", "the", "and", "or", "is", "are", "was", "were", "to", "for",
        "in", "on", "at", "by", "with", "from", "of", "how", "what", "do", "i",
        "my", "me", "you", "he", "she", "it", "they", "we", "can", "should", "please"
    )

    private val BODY_PARTS = mapOf(
        "head" to listOf("head", "skull", "forehead", "scalp"),
        "eye" to listOf("eye", "eyes", "cornea"),
        "throat" to listOf("throat", "airway", "windpipe"),
        "chest" to listOf("chest", "rib", "ribs", "breastbone"),
        "arm" to listOf("arm", "elbow", "wrist", "hand", "finger", "fingers", "palm"),
        "leg" to listOf("leg", "thigh", "knee", "shin", "ankle", "foot", "toe", "toes"),
        "abdomen" to listOf("stomach", "belly", "abdomen", "gut"),
        "back" to listOf("back", "spine")
    )

    // Dedicated multicharacter emergency terms for fuzzy matching (acronyms like CPR excluded)
    private val FUZZY_EMERGENCY_TERMS = setOf(
        "choking", "bleeding", "unconscious", "unresponsive", "breathing",
        "suffocating", "tourniquet", "electrocuted", "anaphylaxis"
    )

    private val HIGH_URGENCY_MODIFIERS = setOf(
        "hurry", "quick", "immediately", "dying", "severe", "gushing", "badly",
        "turning blue", "passed out", "not waking up", "help me"
    )

    /**
     * Complete NLP analysis pipeline on user input.
     */
    fun analyze(query: String): NlpAnalysisResult {
        val trimmed = query.trim().lowercase()
        val rawTokens = trimmed
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val normalizedTokens = rawTokens.filter { it !in STOPWORDS }

        // 1. Detect victim demographic
        val victimType = detectVictimType(rawTokens)

        // 2. Detect anatomical locations
        val detectedBodyParts = detectBodyParts(rawTokens)

        // 3. Classify urgency level
        val urgencyLevel = classifyUrgency(rawTokens, trimmed)

        // 4. Fuzzy spelling correction against emergency terms
        val correctedTokens = rawTokens.map { token ->
            findFuzzyMatch(token, FUZZY_EMERGENCY_TERMS, 1) ?: token
        }
        val correctedQuery = correctedTokens.joinToString(" ")

        return NlpAnalysisResult(
            originalQuery = query,
            normalizedTokens = normalizedTokens,
            victimType = victimType,
            urgencyLevel = urgencyLevel,
            detectedBodyParts = detectedBodyParts,
            detectedSymptoms = rawTokens.filter { it in FUZZY_EMERGENCY_TERMS || it == "cpr" },
            correctedQuery = correctedQuery
        )
    }

    /**
     * Calculates Levenshtein edit distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Checks if a word is a fuzzy match to a target word.
     * Enforces length threshold (>= 5 chars) to prevent false positives like "car" matching "cpr".
     */
    fun isFuzzyMatch(word: String, target: String, maxDistance: Int = 1): Boolean {
        if (word == target) return true
        if (word.length < 5 || target.length < 5) return false
        if (Math.abs(word.length - target.length) > maxDistance) return false
        return levenshteinDistance(word, target) <= maxDistance
    }

    /**
     * Finds the closest match in a dictionary for misspelled terms (length >= 5).
     */
    fun findFuzzyMatch(word: String, dictionary: Set<String>, maxDistance: Int = 1): String? {
        if (word in dictionary) return word
        if (word.length < 5) return null // Guard against small words

        var bestMatch: String? = null
        var bestDist = maxDistance + 1

        for (dictWord in dictionary) {
            if (dictWord.length < 5) continue
            val dist = levenshteinDistance(word, dictWord)
            if (dist < bestDist && dist <= maxDistance) {
                bestDist = dist
                bestMatch = dictWord
            }
        }
        return bestMatch
    }

    private fun detectVictimType(tokens: List<String>): VictimType {
        for (t in tokens) {
            when (t) {
                "baby", "infant", "newborn", "neonatal" -> return VictimType.INFANT
                "child", "kid", "toddler", "boy", "girl", "son", "daughter" -> return VictimType.CHILD
                "adult", "man", "woman", "elderly", "senior", "grandma", "grandpa", "father", "mother" -> return VictimType.ADULT
            }
        }
        return VictimType.UNKNOWN
    }

    private fun detectBodyParts(tokens: List<String>): List<String> {
        val detected = mutableSetOf<String>()
        for (token in tokens) {
            for ((part, aliases) in BODY_PARTS) {
                if (token in aliases) {
                    detected.add(part)
                }
            }
        }
        return detected.toList()
    }

    private fun classifyUrgency(tokens: List<String>, rawText: String): UrgencyLevel {
        if (tokens.any { it in FUZZY_EMERGENCY_TERMS || it == "cpr" }) return UrgencyLevel.CRITICAL
        if (HIGH_URGENCY_MODIFIERS.any { rawText.contains(it) }) return UrgencyLevel.CRITICAL
        if (tokens.any { it in setOf("bleed", "burn", "broken", "fracture", "swollen") }) return UrgencyLevel.URGENT
        return UrgencyLevel.ROUTINE
    }
}
