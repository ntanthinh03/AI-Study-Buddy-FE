package com.thinh.aistudybuddy.data.local

import android.content.Context
import androidx.core.content.edit

data class SessionState(
    val token: String?,
    val rememberLogin: Boolean,
    val displayName: String?
)

object SessionStore {
    private const val PREF_NAME = "ai_study_buddy_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REMEMBER_LOGIN = "remember_login"
    private const val KEY_DISPLAY_NAME = "display_name"

    fun saveSession(context: Context, token: String, rememberLogin: Boolean, displayName: String? = null) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TOKEN, token)
            putBoolean(KEY_REMEMBER_LOGIN, rememberLogin)
            putString(KEY_DISPLAY_NAME, displayName)
        }
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_TOKEN)
            putBoolean(KEY_REMEMBER_LOGIN, false)
            remove(KEY_DISPLAY_NAME)
        }
    }

    fun readSession(context: Context): SessionState {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return SessionState(
            token = prefs.getString(KEY_TOKEN, null),
            rememberLogin = prefs.getBoolean(KEY_REMEMBER_LOGIN, false),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        )
    }
}


