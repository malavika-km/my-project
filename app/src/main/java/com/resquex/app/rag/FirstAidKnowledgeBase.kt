package com.resquex.app.rag

import com.resquex.app.model.FirstAidDocument

/**
 * Authoritative, reviewed local first-aid repository.
 * Grounded in International Red Cross, AHA, and ILCOR (International Liaison Committee on Resuscitation) protocols.
 * Used for offline RAG grounding and zero-AI emergency fallback.
 */
object FirstAidKnowledgeBase {

    const val KNOWLEDGE_BASE_VERSION = 1
    const val LAST_REVIEW_DATE = "2024-11-01"

    val documents: List<FirstAidDocument> = listOf(
        FirstAidDocument(
            id = "DOC-CPR-01",
            category = "Cardiac Arrest",
            title = "Adult CPR and Automated External Defibrillator (AED)",
            keywords = listOf("cpr", "cardiac arrest", "heart attack", "unresponsive", "stopped breathing", "chest compression", "aed", "defibrillator", "pulse"),
            steps = listOf(
                "Verify scene safety. Tap the person's shoulder firmly and shout, 'Are you okay?'",
                "Check for breathing by watching the chest for no more than 10 seconds. If absent or only gasping, immediately call 112/911 (or tell a bystander to call and get an AED).",
                "Place the heel of one hand in the center of the chest (lower half of breastbone). Interlock your other hand on top.",
                "Deliver hard and fast compressions at a rate of 100-120 beats per minute, pressing at least 5 cm (2 inches) deep.",
                "Allow full chest recoil between compressions. Do not lean on the chest.",
                "If trained and willing: Give 30 compressions followed by 2 rescue breaths. If untrained or unwilling: Perform continuous hands-only CPR.",
                "As soon as an AED arrives, turn it on and attach pads as pictured. Follow the voice prompts immediately."
            ),
            warnings = listOf(
                "Do NOT stop chest compressions for more than 10 seconds unless the person clearly wakes up or professional emergency responders take over.",
                "Do NOT perform CPR if the person is awake, breathing normally, or responsive."
            ),
            whenToCallAmbulance = listOf(
                "Person is unresponsive and not breathing or only gasping (Call 112/911 immediately before starting compressions)."
            ),
            sourceName = "International Liaison Committee on Resuscitation (ILCOR) / AHA Guidelines",
            sourceReference = "AHA & ERC 2020/2021 Resuscitation Guidelines",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-BLEED-01",
            category = "Severe Bleeding",
            title = "Management of Severe External Bleeding",
            keywords = listOf("bleed", "bleeding", "blood", "artery", "wound", "cut", "laceration", "hemorrhage", "tourniquet", "pressure dressing"),
            steps = listOf(
                "Ensure personal safety and wear gloves or barrier protection if available.",
                "Expose the wound by removing or cutting clothing if necessary to pinpoint the exact site of bleeding.",
                "Apply continuous, firm direct pressure directly over the bleeding site using a sterile gauze or clean cloth.",
                "If blood soaks through, do NOT remove the original cloth. Add more pads on top and maintain firm downward pressure.",
                "For life-threatening extremity bleeding where direct pressure fails or cannot be maintained, apply a commercial tourniquet 5-7 cm (2-3 inches) proximal to the wound (never over a joint). Tighten until bleeding stops.",
                "Note and write down the exact time of tourniquet application.",
                "Keep the patient calm, lying down, and warm to prevent shock until emergency teams arrive."
            ),
            warnings = listOf(
                "Do NOT remove an embedded object (such as glass, knife, or metal). Stabilize the object with bulky dressings on either side and press around it.",
                "Do NOT loosen or release a tourniquet once applied; only qualified surgical/medical staff should release it."
            ),
            whenToCallAmbulance = listOf(
                "Bleeding is spurting or pulsating.",
                "Bleeding does not stop after 5-10 minutes of direct pressure.",
                "Wound is deep, gaping, or exposes muscle/bone."
            ),
            sourceName = "International Federation of Red Cross and Red Crescent Societies (IFRC)",
            sourceReference = "IFRC International First Aid Guidelines 2020",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-CHOKE-01",
            category = "Choking",
            title = "Airway Obstruction and Choking (Adult and Child over 1 year)",
            keywords = listOf("choking", "choke", "airway", "heimlich", "cannot breathe", "strangling", "abdominal thrusts", "back blows"),
            steps = listOf(
                "Assess severity: Ask 'Are you choking?' If the person can cough forcefully, encourage them to keep coughing.",
                "If the person CANNOT speak, cough, or breathe (silent choking, holding throat with both hands): Call 112/911 immediately.",
                "Stand slightly behind and to the side. Support their chest with one hand and lean the person well forward.",
                "Give up to 5 sharp back blows between the shoulder blades using the heel of your other hand.",
                "If back blows fail, stand directly behind them. Wrap your arms around the waist. Form a fist with one hand and place the thumb side against the abdomen just above the navel.",
                "Grasp the fist with your other hand and deliver up to 5 quick, upward abdominal thrusts (Heimlich maneuver).",
                "Alternate between 5 back blows and 5 abdominal thrusts until the obstruction is expelled or the person becomes unconscious.",
                "If the person becomes unresponsive: Lower them carefully to the floor and immediately begin CPR (30 compressions), checking the mouth for visible objects before each breath."
            ),
            warnings = listOf(
                "Do NOT perform blind finger sweeps in the mouth, which can push foreign objects deeper into the airway.",
                "Do NOT perform abdominal thrusts on infants under 1 year of age (use 5 back blows and 5 chest thrusts instead)."
            ),
            whenToCallAmbulance = listOf(
                "Person is unable to make any sound, turn blue/pale, or loses consciousness."
            ),
            sourceName = "European Resuscitation Council (ERC) & Red Cross",
            sourceReference = "ERC Guidelines for Resuscitation 2021: First Aid",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-BURN-01",
            category = "Burns",
            title = "Thermal and Scald Burn First Aid",
            keywords = listOf("burn", "scald", "heat burn", "fire", "hot water", "blister", "skin peeling", "flame"),
            steps = listOf(
                "Safely remove the patient from the heat source.",
                "Cool the burn immediately under cool or lukewarm running tap water (15°C - 25°C) for at least 20 minutes.",
                "Gently remove clothing, watches, and constricting jewelry from the affected area before swelling starts. Leave clothing that is melted or stuck to the burn intact.",
                "Cover the burn loosely with clean plastic cling film or a sterile, non-adherent dressing to reduce pain and prevent infection.",
                "Keep the person warm with blankets outside the burn area to avoid hypothermia.",
                "Give oral fluids if the patient is fully conscious and transport to hospital if burn is extensive."
            ),
            warnings = listOf(
                "Do NOT use ice, iced water, or frozen items; freezing damages burned tissue and triggers severe hypothermia.",
                "Do NOT break or puncture intact burn blisters.",
                "Do NOT apply butter, toothpaste, grease, oil, flour, or unverified home remedies to burn wounds."
            ),
            whenToCallAmbulance = listOf(
                "Burn is larger than the size of the patient's palm.",
                "Burn involves the face, hands, feet, groin, joints, or encircles a limb/chest.",
                "Skin appears charred (black), white, or leathery (full thickness third-degree burn).",
                "Patient is a child or elderly, or inhaled smoke/hot steam."
            ),
            sourceName = "British Burn Association & International Society for Burn Injuries (ISBI)",
            sourceReference = "ISBI Practice Guidelines for Burn Care 2016/2020",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-ELEC-01",
            category = "Electric Shock",
            title = "Electrical Injury and High-Voltage Safety",
            keywords = listOf("electric", "shock", "electrocuted", "wire", "current", "voltage", "lightning", "socket"),
            steps = listOf(
                "PROTECT YOURSELF FIRST: Do NOT touch the casualty or enter the danger area while current may still be flowing.",
                "Disconnect the power source (switch off circuit breaker, pull plug) if safe to reach.",
                "For low-voltage household current (110-240V), if switch cannot be reached, stand on dry insulating material (rubber mat, dry book) and push source away with dry non-conductive object (wooden broom handle).",
                "For high-voltage power lines: Stay at least 10 meters (33 feet) away. Call 112/911 and emergency utility services immediately.",
                "Once the current is confirmed OFF: Check breathing and responsiveness. If unresponsive and not breathing normally, begin CPR immediately.",
                "Treat visible entry and exit burn wounds with cool water and sterile dressings.",
                "Keep the person lying down and monitor closely; electrical shock can induce delayed cardiac arrhythmias."
            ),
            warnings = listOf(
                "Do NOT touch the victim with bare hands while they remain in contact with an active electric source.",
                "Do NOT approach fallen outdoor electrical wires under any circumstances."
            ),
            whenToCallAmbulance = listOf(
                "All cases of electrical injury require emergency medical evaluation due to invisible internal tissue and cardiac conduction injury."
            ),
            sourceName = "American Heart Association & Red Cross",
            sourceReference = "AHA Focused Update on First Aid 2020",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-STROKE-01",
            category = "Stroke",
            title = "Stroke Identification and Immediate Response (FAST Protocol)",
            keywords = listOf("stroke", "fast", "facial droop", "slurred speech", "arm weakness", "brain attack", "paralysis", "numbness"),
            steps = listOf(
                "Perform the FAST assessment immediately:\n• F (Face): Ask the person to smile. Does one side of the face droop?\n• A (Arms): Ask the person to raise both arms. Does one arm drift downward?\n• S (Speech): Ask the person to repeat a simple sentence. Is speech slurred or strange?\n• T (Time): If any of these signs are present, call 112/911 IMMEDIATELY.",
                "Note the EXACT time the symptoms were first observed (critical for clot-busting medication eligibility).",
                "Keep the person in a comfortable position, ideally lying down with head and shoulders slightly elevated.",
                "Loosen tight clothing around the neck to support breathing.",
                "Do not leave the person alone. Reassure them and monitor consciousness continuously."
            ),
            warnings = listOf(
                "Do NOT give anything to eat or drink (including water or medications like aspirin); stroke impairs swallowing and causes severe choking/aspiration.",
                "Do NOT wait to see if symptoms disappear."
            ),
            whenToCallAmbulance = listOf(
                "Call 112/911 immediately upon noting any single sign of the FAST test."
            ),
            sourceName = "World Stroke Organization (WSO) & American Stroke Association",
            sourceReference = "Guidelines for the Early Management of Patients with Acute Ischemic Stroke",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-ALLERGY-01",
            category = "Allergic Reaction",
            title = "Anaphylaxis and Severe Allergic Emergencies",
            keywords = listOf("allergy", "allergic", "anaphylaxis", "epipen", "epinephrine", "bee sting", "peanut", "swollen lips", "hives", "wheezing"),
            steps = listOf(
                "Recognize anaphylaxis: Rapid swelling of lips, tongue, or throat; difficulty breathing or wheezing; widespread itchy hives; dizziness or collapse.",
                "Call 112/911 immediately and state suspected anaphylaxis.",
                "Ask if the person carries an epinephrine auto-injector (EpiPen / Jext / Anapen).",
                "Help the person administer or administer the auto-injector:\n• Grasp injector with fist (never thumb over end).\n• Pull off safety cap.\n• Push tip firmly into outer mid-thigh at 90-degree angle until a click is heard.\n• Hold firmly in place for 3 to 10 seconds (per device label), then remove.",
                "Massage the injection site for 10 seconds.",
                "Have the person lie flat with legs elevated. If breathing is difficult, allow them to sit up slightly. Do NOT allow them to stand or walk.",
                "If symptoms do not improve within 5-15 minutes and emergency responders have not arrived, a second auto-injector dose may be administered in the opposite thigh."
            ),
            warnings = listOf(
                "Do NOT delay epinephrine administration; delay is the leading cause of fatality in anaphylaxis.",
                "Do NOT have the person stand up quickly, as sudden venous pooling can cause fatal cardiovascular collapse."
            ),
            whenToCallAmbulance = listOf(
                "Any respiratory difficulty, throat tightness, or systemic allergic collapse."
            ),
            sourceName = "World Allergy Organization (WAO) & EAACI",
            sourceReference = "WAO Anaphylaxis Guidelines 2020",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-SEIZURE-01",
            category = "Seizures",
            title = "First Aid for Convulsive Seizures (Epilepsy)",
            keywords = listOf("seizure", "convulsion", "epilepsy", "fit", "shaking", "tremor", "jerking", "foaming at mouth"),
            steps = listOf(
                "Protect the person from injury: Clear sharp objects, hard edges, and furniture away.",
                "Cushion their head with a folded jacket, pillow, or soft item.",
                "Loosen tight neckwear (ties, collars) to ease breathing.",
                "Time the seizure from the moment convulsive movements start.",
                "When the convulsions stop, gently roll the person onto their side into the recovery position to keep airway clear and allow saliva to drain.",
                "Stay with the person until they are fully awake, oriented, and breathing normally. Reassure them calmly."
            ),
            warnings = listOf(
                "Do NOT restrain the person or forcefully hold down their limbs.",
                "Do NOT put ANYTHING in the person's mouth (they will NOT swallow their tongue, and foreign objects break teeth or block the airway).",
                "Do NOT give food, drinks, or medications until completely alert."
            ),
            whenToCallAmbulance = listOf(
                "Seizure lasts longer than 5 minutes.",
                "A second seizure follows immediately without the person regaining consciousness.",
                "The person is pregnant, injured, or has diabetes.",
                "The person has trouble breathing after the convulsions cease.",
                "The seizure occurs in water."
            ),
            sourceName = "Epilepsy Foundation & International League Against Epilepsy (ILAE)",
            sourceReference = "ILAE Clinical Guidance for Seizure First Aid",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-FRACTURE-01",
            category = "Musculoskeletal Trauma",
            title = "Fractures, Dislocations, and Severe Sprains",
            keywords = listOf("fracture", "broken bone", "sprain", "dislocation", "swollen joint", "splint", "deformity", "bone protruding"),
            steps = listOf(
                "Support and immobilize the injured limb in the exact position found. Do not force or realign.",
                "Control any external bleeding with gentle direct pressure around the wound.",
                "If the bone is protruding through the skin (open fracture): Cover with a clean, moist sterile dressing. Do not touch or push bone fragments back.",
                "Apply an improvised splint (using rolled newspapers, boards, or folded blankets) secured with bandages above and below the fracture site.",
                "Check for circulation: Ensure the fingers or toes beyond the injury are warm, pink, and sensation is present.",
                "Apply cold packs wrapped in a cloth to reduce pain and swelling (max 15-20 minutes at a time).",
                "Keep the casualty calm and treat for shock if pale or clammy."
            ),
            warnings = listOf(
                "Do NOT attempt to realign, straighten, or snap bones or joints back into place.",
                "Do NOT test whether the limb can bear weight by asking the person to walk on it."
            ),
            whenToCallAmbulance = listOf(
                "Bone is visible or protruding (open fracture).",
                "Limb is deformed, numb, pale, or has no pulse distal to injury.",
                "Suspected spinal, pelvis, neck, or femur (thigh bone) fracture."
            ),
            sourceName = "American Academy of Orthopaedic Surgeons (AAOS) & Red Cross",
            sourceReference = "AAOS Emergency Care and Transportation of the Sick and Injured",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        ),
        FirstAidDocument(
            id = "DOC-HEAT-01",
            category = "Environmental Emergencies",
            title = "Heat Exhaustion and Life-Threatening Heatstroke",
            keywords = listOf("heatstroke", "heat exhaustion", "sunstroke", "hot weather", "dehydration", "hyperthermia", "cramps"),
            steps = listOf(
                "Recognize difference between Heat Exhaustion (heavy sweating, dizziness, cool pale skin) and Heatstroke (body temp >40°C/104°F, confusion, hot red dry or sweaty skin, altered mental status).",
                "FOR HEATSTROKE (MEDICAL EMERGENCY): Call 112/911 immediately.",
                "Move person to cool, shaded or air-conditioned area immediately.",
                "Cool rapidly by any available means:\n• Immerse body up to neck in cold water if safe and staffed, OR\n• Spray/sponge with cool water and fan aggressively, OR\n• Apply cold/ice packs to neck, armpits, and groin.",
                "For mild/moderate heat exhaustion where patient is fully conscious and alert: Sip cool water or oral rehydration solution slowly.",
                "Monitor consciousness and breathing continuously."
            ),
            warnings = listOf(
                "Do NOT give oral fluids to an individual with altered consciousness, confusion, or vomiting.",
                "Do NOT give antipyretic medications like acetaminophen or aspirin; they are ineffective for environmental heatstroke and can worsen liver/kidney damage."
            ),
            whenToCallAmbulance = listOf(
                "Confusion, seizures, unresponsiveness, or high body temperature (heatstroke)."
            ),
            sourceName = "CDC / NIOSH & Wilderness Medical Society (WMS)",
            sourceReference = "WMS Clinical Practice Guidelines for Heat Illness 2019/2024",
            reviewDate = LAST_REVIEW_DATE,
            version = KNOWLEDGE_BASE_VERSION
        )
    )
}
