package com.example.appmovilstress.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo que muestra el historial de recomendaciones almacenadas para el usuario actual.
 * Cada recomendacion se representa como una tarjeta generada dinamicamente.
 */
class RecommendationsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)

        // Configura el toolbar de la pantalla de recomendaciones.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.recommendations_title))

        // Recupera las recomendaciones del usuario autenticado.
        val database = SQLiteHelper(this)
        val recommendations = database.getRecommendations(SessionManager(this).getUserId())
        val container = findViewById<LinearLayout>(R.id.recommendationsContainer)
        val emptyState = findViewById<TextView>(R.id.textViewEmptyState)

        // Si no hay recomendaciones almacenadas, se muestra un mensaje vacio.
        if (recommendations.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            return
        }

        // Si existen recomendaciones, se crean las tarjetas de forma dinamica.
        emptyState.visibility = View.GONE
        recommendations.forEach { recommendation ->
            val card = layoutInflater.inflate(R.layout.item_recommendation, container, false)
            card.findViewById<TextView>(R.id.textViewRecommendationDate).text = recommendation.fechaGeneracion
            card.findViewById<TextView>(R.id.textViewRecommendationText).text = recommendation.texto

            // Ajusta el color de la fecha para mantener la consistencia visual.
            card.findViewById<TextView>(R.id.textViewRecommendationDate)
                .setTextColor(ContextCompat.getColor(this, R.color.primary_dark))

            container.addView(card)
        }
    }
}
