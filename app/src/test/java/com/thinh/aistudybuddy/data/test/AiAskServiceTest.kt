package com.thinh.aistudybuddy.data.test

import com.thinh.aistudybuddy.data.AiAskRequest
import com.thinh.aistudybuddy.data.AiAskSession
import com.thinh.aistudybuddy.data.AskState
import com.thinh.aistudybuddy.data.ChatResponse
import com.thinh.aistudybuddy.data.DocumentStatusResponse
import com.thinh.aistudybuddy.data.RagUploadResponse
import com.thinh.aistudybuddy.data.local.NetworkConfigStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAskServiceTest {

    @Test
    fun normalizeBaseUrl_addsSchemeAndTrailingSlash() {
        assertEquals("http://10.0.2.2:3001/", NetworkConfigStore.normalizeBaseUrl("10.0.2.2:3001"))
        assertEquals("https://example.com/", NetworkConfigStore.normalizeBaseUrl("https://example.com"))
    }

    @Test
    fun normalizeBaseUrl_returnsNullForBlankInput() {
        assertNull(NetworkConfigStore.normalizeBaseUrl("   "))
        assertNull(NetworkConfigStore.normalizeBaseUrl(null))
    }

    @Test
    fun aiAskSession_defaultsToIdleState() {
        val session = AiAskSession(id = "s1", question = "What is Kotlin?")

        assertEquals("s1", session.id)
        assertEquals("What is Kotlin?", session.question)
        assertEquals(AskState.IDLE, session.state)
        assertEquals(0, session.retryCount)
        assertTrue(session.answer.isEmpty())
        assertNull(session.documentId)
    }

    @Test
    fun models_holdExpectedDefaults() {
        val request = AiAskRequest(question = "Explain coroutines")
        val chatResponse = ChatResponse(answer = "Coroutines are lightweight threads.")
        val uploadResponse = RagUploadResponse(success = true)
        val statusResponse = DocumentStatusResponse(id = "doc-1", status = "PROCESSING")

        assertEquals("Explain coroutines", request.question)
        assertEquals("Coroutines are lightweight threads.", chatResponse.answer)
        assertEquals("PROCESSING", uploadResponse.status)
        assertTrue(uploadResponse.success)
        assertEquals("doc-1", statusResponse.id)
        assertEquals("PROCESSING", statusResponse.status)
    }
}


