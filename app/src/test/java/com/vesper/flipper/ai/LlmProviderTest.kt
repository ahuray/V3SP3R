package com.vesper.flipper.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmProviderTest {

    @Test
    fun unknown_provider_id_stays_on_openrouter() {
        assertEquals(LlmProvider.OPENROUTER, LlmProvider.fromId(null))
        assertEquals(LlmProvider.OPENROUTER, LlmProvider.fromId("not-a-provider"))
    }

    @Test
    fun each_provider_keeps_its_own_endpoint_and_stored_keys() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            LlmProvider.OPENROUTER.chatCompletionsUrl
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            LlmProvider.GEMINI.chatCompletionsUrl
        )
        assertEquals("https://api.openai.com/v1/chat/completions", LlmProvider.OPENAI.chatCompletionsUrl)
        assertEquals("https://api.x.ai/v1/chat/completions", LlmProvider.XAI.chatCompletionsUrl)
        assertEquals(
            "https://api.anthropic.com/v1/chat/completions",
            LlmProvider.ANTHROPIC.chatCompletionsUrl
        )

        assertEquals("openrouter_api_key", LlmProvider.OPENROUTER.apiKeyPreference)
        assertEquals("selected_model", LlmProvider.OPENROUTER.modelPreference)
        assertEquals("gemini_api_key", LlmProvider.GEMINI.apiKeyPreference)
        assertEquals("openai_api_key", LlmProvider.OPENAI.apiKeyPreference)
        assertEquals("xai_api_key", LlmProvider.XAI.apiKeyPreference)
        assertEquals("anthropic_api_key", LlmProvider.ANTHROPIC.apiKeyPreference)
        assertEquals("selected_model_anthropic", LlmProvider.ANTHROPIC.modelPreference)
        assertEquals("claude-sonnet-5-5", LlmProvider.ANTHROPIC.defaultModel)
        assertFalse(LlmProvider.ANTHROPIC.allowsSamplingTemperature)
        assertFalse(LlmProvider.ANTHROPIC.sendsAttributionHeaders)
        assertTrue(LlmProvider.ANTHROPIC.acceptsCatalogModel("claude-sonnet-5-5"))
        assertFalse(LlmProvider.ANTHROPIC.acceptsCatalogModel("claude-3-haiku-tts"))
    }

    @Test
    fun tool_fallbacks_never_cross_providers() {
        val gemini = LlmProvider.GEMINI.toolCandidates("gemini-3.1-pro-preview")
        assertEquals("gemini-3.1-pro-preview", gemini.first())
        assertTrue(gemini.all { it.startsWith("gemini-") })
        assertFalse(gemini.any { it.contains("/") })

        val openai = LlmProvider.OPENAI.toolCandidates("gpt-6-astra")
        assertEquals("gpt-6-astra", openai.first())
        assertTrue(openai.none { it.startsWith("grok-") || it.contains("/") })

        val xai = LlmProvider.XAI.toolCandidates("grok-4.3")
        assertEquals("grok-4.3", xai.first())
        assertTrue(xai.all { it.startsWith("grok-") })

        val openRouter = LlmProvider.OPENROUTER.toolCandidates("anthropic/claude-sonnet-4.5")
        assertEquals("anthropic/claude-sonnet-4.5", openRouter.first())
        assertFalse(openRouter.contains("gemini-3.8-flash"))
        assertFalse(openRouter.contains("google/gemini-2.5-flash-image-preview"))
    }

    @Test
    fun openai_uses_completion_token_limit_and_skips_custom_temperature() {
        val limits = LlmProvider.OPENAI.completionLimits(1024)
        assertNull(limits.maxTokens)
        assertEquals(1024, limits.maxCompletionTokens)
        assertFalse(LlmProvider.OPENAI.allowsSamplingTemperature)

        val gemini = LlmProvider.GEMINI.completionLimits(720)
        assertEquals(720, gemini.maxTokens)
        assertNull(gemini.maxCompletionTokens)
        assertTrue(LlmProvider.GEMINI.allowsSamplingTemperature)
        assertTrue(LlmProvider.XAI.allowsSamplingTemperature)
    }

    @Test
    fun catalog_filter_drops_speech_image_and_embedding_models() {
        assertEquals("gemini-3.8-flash", LlmProvider.GEMINI.normalizeModelId("models/gemini-3.8-flash"))
        assertTrue(LlmProvider.GEMINI.acceptsCatalogModel("models/gemini-3.8-flash"))
        assertFalse(LlmProvider.GEMINI.acceptsCatalogModel("models/gemini-3.8-flash-tts"))
        assertFalse(LlmProvider.GEMINI.acceptsCatalogModel("gemini-3.1-flash-image"))

        assertTrue(LlmProvider.OPENAI.acceptsCatalogModel("gpt-6.1-sol"))
        assertFalse(LlmProvider.OPENAI.acceptsCatalogModel("gpt-4o-mini-tts"))
        assertFalse(LlmProvider.OPENAI.acceptsCatalogModel("text-embedding-3-large"))

        assertTrue(LlmProvider.XAI.acceptsCatalogModel("grok-4.7"))
        assertFalse(LlmProvider.XAI.acceptsCatalogModel("grok-imagine-image-2.0"))
        assertFalse(LlmProvider.XAI.acceptsCatalogModel("grok-voice-think-fast-2.0"))
    }

    @Test
    fun live_catalog_keeps_curated_models_and_skips_dated_snapshots() {
        val merged = LlmProvider.mergeCatalog(
            LlmProvider.OPENAI,
            listOf("gpt-6.1-sol", "gpt-4o-2024-08-06", "gpt-5.4", "whisper-1")
        )
        assertEquals("gpt-6.1-sol", merged.first().id)
        assertTrue(merged.any { it.id == "gpt-5.4" })
        assertFalse(merged.any { it.id == "gpt-4o-2024-08-06" })
        assertFalse(merged.any { it.id == "whisper-1" })
    }

    @Test
    fun voice_falls_back_to_the_provider_default_when_the_saved_id_belongs_elsewhere() {
        assertEquals("shimmer", LlmProvider.OPENAI.voiceOrDefault("shimmer").id)
        assertEquals("Kore", LlmProvider.GEMINI.voiceOrDefault("shimmer").id)
        assertEquals("eve", LlmProvider.XAI.voiceOrDefault("Kore").id)
    }

    @Test
    fun only_openrouter_sends_attribution_headers() {
        assertTrue(LlmProvider.OPENROUTER.sendsAttributionHeaders)
        assertFalse(LlmProvider.GEMINI.sendsAttributionHeaders)
        assertFalse(LlmProvider.OPENAI.sendsAttributionHeaders)
        assertFalse(LlmProvider.XAI.sendsAttributionHeaders)
        assertEquals(
            "Cannot resolve api.x.ai (DNS/network issue). Verify internet access, disable broken Private DNS/VPN, then retry.",
            LlmProvider.dnsFailureMessage("api.x.ai")
        )
    }
}
