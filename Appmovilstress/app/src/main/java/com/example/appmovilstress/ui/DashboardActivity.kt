package com.example.appmovilstress.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.appmovilstress.R
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo correspondiente al menu principal de la aplicacion.
 * Desde aqui el usuario accede al cuestionario, resultados, evolucion y cierre de sesion.
 */
class DashboardActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Inicializa la sesion y comprueba que el usuario este autenticado.
        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            navigateToWelcome()
            return
        }

        // Configura el toolbar y elimina el boton de volver en el menu principal.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), "Menu principal", showBackButton = false)

        // Muestra un mensaje de bienvenida con el nombre del usuario autenticado.
        findViewById<TextView>(R.id.textViewWelcome).text =
            "Bienvenido/a, ${sessionManager.getUserName()}"

        // Abre la pantalla del cuestionario PSS-14.
        findViewById<Button>(R.id.buttonStartQuestionnaire).setOnClickListener {
            startActivity(Intent(this, QuestionnaireActivity::class.java))
        }

        // Abre la pantalla con el ultimo resultado disponible.
        findViewById<Button>(R.id.buttonLastResult).setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_SHOW_LATEST, true)
            }
            startActivity(intent)
        }

        // Abre la pantalla con la evolucion temporal del estres.
        findViewById<Button>(R.id.buttonEvolution).setOnClickListener {
            startActivity(Intent(this, EvolutionActivity::class.java))
        }

        // Abre la pantalla con el historial de recomendaciones generadas.
        findViewById<Button>(R.id.buttonRecommendations).setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        // Permite cerrar sesion tras confirmar la accion en un dialogo.
        findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("Quieres cerrar la sesion actual?")
                .setPositiveButton("Si") { _, _ ->
                    sessionManager.clearSession()
                    navigateToWelcome()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // Redirige a la portada y vacia el historial de pantallas abiertas.
    private fun navigateToWelcome() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
