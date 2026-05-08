package com.thinh.aistudybuddy.data.local

import android.content.Context

/**
 * Lightweight token persistence using SharedPreferences as a compatibility fallback.
 * Using SharedPreferences avoids adding a DataStore dependency during initial fixes.
 */
object TokenDataStore {
    private const val PREF_NAME = "ai_study_buddy_token"
    private const val KEY_AUTH_TOKEN = "auth_token"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    fun readToken(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_TOKEN, null)
    }
}


