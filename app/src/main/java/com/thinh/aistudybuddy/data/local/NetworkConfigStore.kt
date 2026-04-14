package com.thinh.aistudybuddy.data.local

import android.content.Context
import androidx.core.content.edit

object NetworkConfigStore {
    private const val PREF_NAME = "ai_study_buddy_network"
    private const val KEY_BASE_URL = "base_url_override"

    fun saveBaseUrl(context: Context, baseUrl: String?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_BASE_URL, normalizeBaseUrl(baseUrl))
        }
    }

    fun clearBaseUrl(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_BASE_URL)
        }
    }

    fun readBaseUrl(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun normalizeBaseUrl(baseUrl: String?): String? {
        val value = baseUrl?.trim().orEmpty()
        if (value.isBlank()) return null
        val withScheme = if (value.startsWith("http://") || value.startsWith("https://")) value else "http://$value"
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}

