package com.example.appmovilstress.service

import android.content.Context

/*
 * Archivo encargado de gestionar la sesion local del usuario mediante SharedPreferences.
 * Permite guardar el usuario autenticado y comprobar si existe una sesion activa.
 */
class SessionManager(context: Context) {

    // Preferencias privadas donde se guarda el identificador del usuario autenticado.
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Guarda el identificador y el nombre del usuario para mantener la sesion iniciada.
    fun saveUserSession(userId: Long, userName: String) {
        preferences.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    // Recupera el identificador del usuario o -1 si no existe sesion.
    fun getUserId(): Long = preferences.getLong(KEY_USER_ID, -1L)

    // Recupera el nombre del usuario autenticado.
    fun getUserName(): String = preferences.getString(KEY_USER_NAME, "") ?: ""

    // Determina si la aplicacion tiene una sesion valida almacenada.
    fun isLoggedIn(): Boolean = getUserId() != -1L

    // Elimina todos los datos de sesion cuando el usuario cierra sesion.
    fun clearSession() {
        preferences.edit().clear().apply()
    }

    companion object {
        // Nombre del archivo de preferencias utilizado para la sesion.
        private const val PREF_NAME = "stress_monitor_session"

        // Claves internas utilizadas para guardar cada dato de sesion.
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
    }
}
