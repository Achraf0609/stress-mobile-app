package com.example.appmovilstress.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo que gestiona el inicio de sesion del usuario.
 * Valida las credenciales contra la base de datos local y crea una sesion persistente.
 */
class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configura el toolbar de la pantalla de login.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.login))

        // Prepara los accesos a la base de datos, sesion y campos del formulario.
        val database = SQLiteHelper(this)
        val sessionManager = SessionManager(this)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)

        findViewById<Button>(R.id.buttonDoLogin).setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validacion de credenciales introducidas por el usuario antes de consultar la base de datos.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Introduce email y contrasena.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificacion de email y contrasena en SQLite. Si son correctos, se crea la sesion local.
            val user = database.login(email, password)
            if (user != null) {
                sessionManager.saveUserSession(user.id, user.nombre)
                startActivity(Intent(this, DashboardActivity::class.java))
                finishAffinity()
            } else {
                Toast.makeText(this, "Credenciales incorrectas.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
