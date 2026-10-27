package com.resquex.app

import com.resquex.app.llm.QwenChatTemplate
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Qwen ChatML template formatting.
 */
class QwenChatTemplateTest {

    @Test
    fun testChatMlFormatCorrectness() {
        val formatted = QwenChatTemplate.format(
            systemPrompt = "You are an emergency assistant.",
            userMessage = "What is the CPR ratio?"
        )

        assertTrue(formatted.startsWith("<|im_start|>system\nYou are an emergency assistant.\n<|im_end|>\n"))
        assertTrue(formatted.contains("<|im_start|>user\nWhat is the CPR ratio?\n<|im_end|>\n"))
        assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun testChatMlWithoutSystemPrompt() {
        val formatted = QwenChatTemplate.format(
            systemPrompt = "",
            userMessage = "Help with a burn"
        )

        assertFalse(formatted.contains("<|im_start|>system"))
        assertTrue(formatted.startsWith("<|im_start|>user\nHelp with a burn\n<|im_end|>\n"))
        assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
    }
}
