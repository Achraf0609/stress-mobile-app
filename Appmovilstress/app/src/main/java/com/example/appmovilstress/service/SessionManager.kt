package com.example.appmovilstress.service

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(userId: Long, userName: String) {
        preferences.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    fun getUserId(): Long = preferences.getLong(KEY_USER_ID, -1L)

    fun getUserName(): String = preferences.getString(KEY_USER_NAME, "") ?: ""

    fun isLoggedIn(): Boolean = getUserId() != -1L

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "stress_monitor_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
    }
}
