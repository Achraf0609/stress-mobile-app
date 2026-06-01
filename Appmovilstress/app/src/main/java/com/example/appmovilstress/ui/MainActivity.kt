package com.example.appmovilstress.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.appmovilstress.R
import com.example.appmovilstress.service.SessionManager

/*
 * Archivo que representa la pantalla inicial de la aplicacion.
 * Desde aqui el usuario puede registrarse, iniciar sesion o ser redirigido si ya tiene sesion activa.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprueba si ya existe una sesion iniciada para evitar mostrar la portada innecesariamente.
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        // Carga el layout principal con los accesos a login y registro.
        setContentView(R.layout.activity_main)

        // Navega a la pantalla de inicio de sesion.
        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Navega a la pantalla de registro de nuevos usuarios.
        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
