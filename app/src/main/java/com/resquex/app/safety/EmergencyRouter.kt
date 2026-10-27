package com.resquex.app.safety

import com.resquex.app.model.EmergencyActionCard
import com.resquex.app.nlp.EmergencyNlpProcessor

/**
 * Result of the deterministic safety evaluation performed before any AI model invocation.
 */
sealed class SafetyCheckResult {
    /**
     * Life-threatening emergency identified. The attached action card must be rendered
     * immediately with a direct one-tap emergency call option, without waiting for model generation.
     */
    data class CriticalEmergency(
        val card: EmergencyActionCard
    ) : SafetyCheckResult()

    /**
     * Query is outside supported emergency first-aid scope (e.g. drug prescription, formal diagnosis, harmful acts).
     */
    data class BlockedUnsafe(
        val reason: String,
        val safeAdvisory: String
    ) : SafetyCheckResult()

    /**
     * Query is safe and appropriate for grounded first-aid retrieval and Qwen/Cloud inference.
     */
    object ProceedToAi : SafetyCheckResult()
}

/**
 * Deterministic safety classifier and emergency triage router.
 * Evaluates rules offline with zero network or model latency.
 */
object EmergencyRouter {

    const val DEFAULT_EMERGENCY_NUMBER = "112" // Universal GSM emergency number; dials 911/local in appropriate regions

    private val CARDIAC_PATTERNS = listOf(
        Regex("\\b(cpr|cardiac arrest|heart attack|chest pain|not breathing|stopped breathing|no pulse|unresponsive|collapsed and not breathing)\\b", RegexOption.IGNORE_CASE)
    )

    private val BLEEDING_PATTERNS = listOf(
        Regex("\\b(severe bleeding|bleeding heavily|spurting blood|cut artery|gushing blood|tourniquet|stab wound bleeding|deep laceration)\\b", RegexOption.IGNORE_CASE)
    )

    private val CHOKING_PATTERNS = listOf(
        Regex("\\b(choking|choked|cannot breathe food|can't breathe food|airway blocked|heimlich|strangled)\\b", RegexOption.IGNORE_CASE)
    )

    private val BURNS_PATTERNS = listOf(
        Regex("\\b(severe burn|third degree burn|skin charred|chemical burn|acid burn|boiling water scald|body on fire)\\b", RegexOption.IGNORE_CASE)
    )

    private val ELECTRIC_PATTERNS = listOf(
        Regex("\\b(electric shock|electrocuted|high voltage|power line shock|touched live wire)\\b", RegexOption.IGNORE_CASE)
    )

    private val ANAPHYLAXIS_PATTERNS = listOf(
        Regex("\\b(anaphylaxis|throat swelling|throat closing|epipen|severe allergic reaction|tongue swelling cannot breathe)\\b", RegexOption.IGNORE_CASE)
    )

    private val STROKE_PATTERNS = listOf(
        Regex("\\b(stroke|face drooping|facial droop|slurred speech|arm numbness fast|fast stroke)\\b", RegexOption.IGNORE_CASE)
    )

    private val POISONING_PATTERNS = listOf(
        Regex("\\b(swallowed poison|drank bleach|overdose|ingested chemicals|poison control)\\b", RegexOption.IGNORE_CASE)
    )

    // Out-of-scope / high-risk medical query patterns
    private val PRESCRIPTION_PATTERNS = listOf(
        Regex("\\b(prescribe|how many mg|what dosage|take how many pills|antibiotic dose|insulin dose|chemotherapy)\\b", RegexOption.IGNORE_CASE)
    )

    private val DIAGNOSIS_PATTERNS = listOf(
        Regex("\\b(diagnose me|do i have cancer|is this tumor|interpret my blood test|interpret my mri|diagnose illness)\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Inspects input query and determines whether to trigger an emergency card, block, or proceed to AI.
     * Evaluates exact patterns first, then applies NLP typo correction for panic-induced misspellings.
     */
    fun evaluate(query: String): SafetyCheckResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return SafetyCheckResult.BlockedUnsafe(
                reason = "Empty input",
                safeAdvisory = "Please enter an emergency first-aid question."
            )
        }

        // 1. High-risk prescription / dosage screening
        if (PRESCRIPTION_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return SafetyCheckResult.BlockedUnsafe(
                reason = "Medication dosage/prescription requested",
                safeAdvisory = "RESQUE-X provides emergency first-aid instructions only and cannot prescribe medications or determine dosages. Please consult a licensed doctor, pharmacist, or poison control center."
            )
        }

        // 2. High-risk diagnostic screening
        if (DIAGNOSIS_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return SafetyCheckResult.BlockedUnsafe(
                reason = "Medical diagnosis requested",
                safeAdvisory = "RESQUE-X is not a diagnostic tool and cannot diagnose medical diseases or interpret laboratory results. Please consult a qualified physician for medical diagnosis."
            )
        }

        // Evaluate on original text first
        val directMatch = evaluatePatterns(trimmed)
        if (directMatch != null) return directMatch

        // If no direct match, check with NLP typo correction (for panic typos like "chokng", "bleading")
        val nlpAnalysis = EmergencyNlpProcessor.analyze(trimmed)
        if (nlpAnalysis.correctedQuery != trimmed) {
            val correctedMatch = evaluatePatterns(nlpAnalysis.correctedQuery)
            if (correctedMatch != null) return correctedMatch
        }

        // Safe for standard AI generation
        return SafetyCheckResult.ProceedToAi
    }

    private fun evaluatePatterns(text: String): SafetyCheckResult? {
        // CARDIAC ARREST & CPR
        if (CARDIAC_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Cardiac Arrest",
                    title = "CRITICAL: CARDIAC ARREST & CPR PROTOCOL",
                    immediateSteps = listOf(
                        "CALL 112 / 911 IMMEDIATELY or direct a bystander to call and get an AED.",
                        "Check if the person responds or is breathing normally (watch chest for 10 seconds).",
                        "If NOT breathing or only gasping: Place the heel of one hand in the center of the chest, interlock fingers with other hand.",
                        "Push HARD and FAST: 100-120 compressions per minute, at least 5 cm (2 inches) deep.",
                        "Allow complete chest recoil between compressions.",
                        "Apply and turn on the AED as soon as it arrives, following audio prompts."
                    ),
                    warnings = listOf(
                        "Do NOT stop compressions until emergency medics arrive or person regains consciousness.",
                        "Do NOT delay compressions to search for pulse if untrained."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "ILCOR / American Heart Association Resuscitation Guidelines"
                )
            )
        }

        // SEVERE BLEEDING
        if (BLEEDING_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Severe Bleeding",
                    title = "CRITICAL: SEVERE BLEEDING CONTROL",
                    immediateSteps = listOf(
                        "CALL 112 / 911 immediately if blood is spurting or does not stop.",
                        "Apply direct, firm pressure on the wound with sterile gauze or clean cloth.",
                        "Maintain continuous pressure. If blood soaks through, do NOT remove cloth; add more pads on top.",
                        "For severe extremity bleeding that cannot be controlled, apply a tourniquet 5-7 cm above the wound.",
                        "Keep the victim warm and lying flat to treat for hemorrhagic shock."
                    ),
                    warnings = listOf(
                        "Do NOT remove embedded objects (knife, glass) — stabilize them in place and apply pressure around them.",
                        "Do NOT remove tourniquet once applied."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "International Federation of Red Cross First Aid Guidelines"
                )
            )
        }

        // CHOKING
        if (CHOKING_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Choking",
                    title = "CRITICAL: AIRWAY OBSTRUCTION / CHOKING",
                    immediateSteps = listOf(
                        "CALL 112 / 911 immediately if the person cannot speak, cough, or breathe.",
                        "Lean the person forward and give up to 5 sharp BACK BLOWS between the shoulder blades with heel of hand.",
                        "If still choking, stand behind them, place fist just above the navel, and deliver up to 5 upward ABDOMINAL THRUSTS (Heimlich).",
                        "Repeat cycles of 5 back blows and 5 abdominal thrusts.",
                        "If person becomes unresponsive: Lower to floor and begin CPR immediately."
                    ),
                    warnings = listOf(
                        "Do NOT perform blind finger sweeps in the mouth.",
                        "Do NOT perform abdominal thrusts on infants under 1 year (use 5 back blows + 5 chest thrusts)."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "European Resuscitation Council (ERC) First Aid Protocol"
                )
            )
        }

        // SEVERE BURNS
        if (BURNS_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Burns",
                    title = "CRITICAL: SEVERE BURN MANAGEMENT",
                    immediateSteps = listOf(
                        "CALL 112 / 911 immediately for extensive, electrical, or chemical burns.",
                        "Cool the burn under cool, clean running water for at least 20 minutes.",
                        "Gently remove non-adherent clothing and jewelry before swelling begins.",
                        "Cover loosely with sterile non-adherent dressing or clean cling film.",
                        "Keep person warm to prevent hypothermia."
                    ),
                    warnings = listOf(
                        "Do NOT apply ice, iced water, butter, oils, or toothpaste.",
                        "Do NOT pop blisters or remove clothing stuck to burned skin."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "International Society for Burn Injuries (ISBI)"
                )
            )
        }

        // ELECTRIC SHOCK
        if (ELECTRIC_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Electric Shock",
                    title = "CRITICAL: ELECTRICAL SHOCK PROTOCOL",
                    immediateSteps = listOf(
                        "DO NOT TOUCH THE CASUALTY while connected to an active power source.",
                        "Turn off the power / circuit breaker immediately.",
                        "CALL 112 / 911 immediately.",
                        "Once safe, check breathing and responsiveness; begin CPR if not breathing normally.",
                        "Treat entry and exit burn marks with cool running water."
                    ),
                    warnings = listOf(
                        "Do NOT approach downed power lines (maintain at least 10 meters distance).",
                        "Do NOT use conductive objects or wet materials to move wires."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "American Heart Association Emergency Guidelines"
                )
            )
        }

        // ANAPHYLAXIS
        if (ANAPHYLAXIS_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Anaphylaxis",
                    title = "CRITICAL: SEVERE ALLERGIC ANAPHYLAXIS",
                    immediateSteps = listOf(
                        "CALL 112 / 911 IMMEDIATELY and report severe anaphylaxis.",
                        "Inject epinephrine auto-injector (EpiPen) into the outer mid-thigh at 90 degrees.",
                        "Hold in place for 3-10 seconds as directed on device, then remove.",
                        "Have the person lie flat with legs elevated (sit up slightly only if breathing is difficult).",
                        "Do NOT allow them to stand up or walk."
                    ),
                    warnings = listOf(
                        "Do NOT delay epinephrine administration — delay can be fatal.",
                        "Do NOT give oral antihistamines as a substitute for epinephrine in anaphylaxis."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "World Allergy Organization (WAO) Anaphylaxis Guidelines"
                )
            )
        }

        // STROKE
        if (STROKE_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Stroke",
                    title = "CRITICAL: STROKE EMERGENCY (FAST TEST)",
                    immediateSteps = listOf(
                        "CALL 112 / 911 IMMEDIATELY if any FAST sign is present:\n• Face: Face drooping on one side?\n• Arms: One arm drifting down when raised?\n• Speech: Slurred or strange speech?\n• Time: Note exact time symptoms began and call emergency now.",
                        "Keep person lying down with head/shoulders slightly elevated.",
                        "Loosen tight clothing around neck."
                    ),
                    warnings = listOf(
                        "Do NOT give food, drink, or aspirin (swallowing reflex may be paralyzed, causing choking).",
                        "Do NOT wait to see if symptoms improve."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "World Stroke Organization (WSO)"
                )
            )
        }

        // POISONING
        if (POISONING_PATTERNS.any { it.containsMatchIn(text) }) {
            return SafetyCheckResult.CriticalEmergency(
                card = EmergencyActionCard(
                    category = "Poisoning",
                    title = "CRITICAL: SUSPECTED POISONING / OVERDOSE",
                    immediateSteps = listOf(
                        "CALL 112 / 911 and your National Poison Control Center immediately.",
                        "Keep the container, package, or remaining substance for medical responders to identify.",
                        "If the person is unconscious or having seizures, place in recovery position and monitor breathing.",
                        "Begin CPR immediately if they stop breathing normally."
                    ),
                    warnings = listOf(
                        "Do NOT induce vomiting unless specifically instructed by Poison Control.",
                        "Do NOT give milk, activated charcoal, or home remedies without explicit poison center authorization."
                    ),
                    emergencyNumber = DEFAULT_EMERGENCY_NUMBER,
                    sourceCitation = "American Association of Poison Control Centers (AAPCC)"
                )
            )
        }

        return null
    }
}
