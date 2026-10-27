package com.resquex.app

import com.resquex.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying offline chat workflow data structures and lifecycle models.
 */
class OfflineChatWorkflowTest {

    @Test
    fun testChatMessageCreationAndImmutability() {
        val userMsg = ChatMessage(
            text = "How to treat a burn?",
            sender = MessageSender.USER
        )

        assertEquals("How to treat a burn?", userMsg.text)
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals(MessageStatus.SUCCESS, userMsg.status)
        assertTrue(userMsg.timestamp > 0)
        assertNotNull(userMsg.id)

        val assistantMsg = ChatMessage(
            text = "Cool with water for 20 minutes.",
            sender = MessageSender.ASSISTANT,
            source = MessageSource.OFFLINE_QWEN,
            citations = listOf("International Red Cross 2020")
        )

        assertEquals(MessageSource.OFFLINE_QWEN, assistantMsg.source)
        assertEquals(1, assistantMsg.citations.size)
        assertEquals("International Red Cross 2020", assistantMsg.citations.first())

        val multimodalMsg = ChatMessage(
            text = "Analyze this burn wound",
            sender = MessageSender.USER,
            imageBase64 = "base64dummydata==",
            imageUri = "content://media/images/1"
        )
        assertEquals("base64dummydata==", multimodalMsg.imageBase64)
        assertEquals("content://media/images/1", multimodalMsg.imageUri)
    }

    @Test
    fun testEmergencyActionCardFormat() {
        val card = EmergencyActionCard(
            category = "Cardiac Arrest",
            title = "CRITICAL: CARDIAC ARREST & CPR",
            immediateSteps = listOf("Call 112", "Start 100-120 compressions/min"),
            warnings = listOf("Do not stop compressions"),
            emergencyNumber = "112",
            sourceCitation = "ILCOR 2021"
        )

        assertEquals("Cardiac Arrest", card.category)
        assertEquals(2, card.immediateSteps.size)
        assertEquals("112", card.emergencyNumber)
        assertEquals("ILCOR 2021", card.sourceCitation)
    }

    @Test
    fun testModelStatusStates() {
        val notInstalled: ModelStatus = ModelStatus.NotInstalled
        assertTrue(notInstalled is ModelStatus.NotInstalled)

        val downloading: ModelStatus = ModelStatus.Downloading(0.45f, 45000000L, 100000000L)
        assertTrue(downloading is ModelStatus.Downloading)
        assertEquals(0.45f, (downloading as ModelStatus.Downloading).progress, 0.001f)

        val ready: ModelStatus = ModelStatus.Ready(File("dummy.gguf"), 398000000L)
        assertTrue(ready is ModelStatus.Ready)
        assertEquals(398000000L, (ready as ModelStatus.Ready).sizeBytes)

        val error: ModelStatus = ModelStatus.Error("Checksum mismatch")
        assertTrue(error is ModelStatus.Error)
        assertEquals("Checksum mismatch", (error as ModelStatus.Error).message)
    }

    @Test
    fun testFirstAidDocumentGroundedContextFormat() {
        val doc = FirstAidDocument(
            id = "TEST-01",
            category = "Burns",
            title = "Thermal Burn First Aid",
            keywords = listOf("burn", "scald"),
            steps = listOf("Cool under tap water for 20 minutes", "Cover with cling film"),
            warnings = listOf("Do not apply ice", "Do not burst blisters"),
            whenToCallAmbulance = listOf("Skin is charred black"),
            sourceName = "British Burn Association",
            sourceReference = "ISBI Guidelines 2020",
            reviewDate = "2024-11-01",
            version = 1
        )

        val formatted = doc.toGroundedContextString()
        assertTrue(formatted.contains("[Topic: Thermal Burn First Aid]"))
        assertTrue(formatted.contains("1. Cool under tap water for 20 minutes"))
        assertTrue(formatted.contains("- Do not apply ice"))
        assertTrue(formatted.contains("Skin is charred black"))
        assertTrue(formatted.contains("Source: British Burn Association (ISBI Guidelines 2020)"))
    }
}
