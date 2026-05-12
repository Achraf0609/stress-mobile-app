package com.example.appmovilstress.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.appmovilstress.R
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

class DashboardActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            navigateToWelcome()
            return
        }

        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), "Menú principal", showBackButton = false)

        findViewById<TextView>(R.id.textViewWelcome).text =
            "Bienvenido/a, ${sessionManager.getUserName()}"

        findViewById<Button>(R.id.buttonStartQuestionnaire).setOnClickListener {
            startActivity(Intent(this, QuestionnaireActivity::class.java))
        }

        findViewById<Button>(R.id.buttonLastResult).setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_SHOW_LATEST, true)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.buttonEvolution).setOnClickListener {
            startActivity(Intent(this, EvolutionActivity::class.java))
        }

        findViewById<Button>(R.id.buttonRecommendations).setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar la sesión actual?")
                .setPositiveButton("Sí") { _, _ ->
                    sessionManager.clearSession()
                    navigateToWelcome()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun navigateToWelcome() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
