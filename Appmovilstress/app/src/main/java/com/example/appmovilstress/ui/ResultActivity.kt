package com.example.appmovilstress.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.ResultadoConRecomendacion
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo que muestra el resultado del cuestionario y la recomendacion asociada.
 * Puede presentar el resultado recien calculado o recuperar el ultimo resultado guardado.
 */
class ResultActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Configura el toolbar de la pantalla de resultados.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.result_title))

        val emptyState = findViewById<TextView>(R.id.textViewEmptyState)
        val contentGroup = findViewById<View>(R.id.resultContent)

        // Si la pantalla recibe datos por Intent, se muestran directamente sin consultar SQLite.
        val scoreFromIntent = intent.getIntExtra(EXTRA_SCORE, -1)
        if (scoreFromIntent != -1) {
            emptyState.visibility = View.GONE
            contentGroup.visibility = View.VISIBLE
            bindResult(
                ResultadoConRecomendacion(
                    resultadoId = intent.getLongExtra(EXTRA_RESULT_ID, -1),
                    userId = SessionManager(this).getUserId(),
                    puntuacion = scoreFromIntent,
                    nivelEstres = intent.getStringExtra(EXTRA_LEVEL).orEmpty(),
                    fecha = intent.getStringExtra(EXTRA_DATE).orEmpty(),
                    recomendacion = intent.getStringExtra(EXTRA_RECOMMENDATION).orEmpty()
                )
            )
            return
        }

        // Si no hay datos en el Intent, se recupera el ultimo resultado guardado del usuario.
        val latest = SQLiteHelper(this).getLastResult(SessionManager(this).getUserId())
        if (latest == null) {
            emptyState.visibility = View.VISIBLE
            contentGroup.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            contentGroup.visibility = View.VISIBLE
            bindResult(latest)
        }
    }

    // Rellena la interfaz con la puntuacion, nivel, recomendacion y fecha del resultado.
    private fun bindResult(result: ResultadoConRecomendacion) {
        findViewById<TextView>(R.id.textViewScore).text = result.puntuacion.toString()
        findViewById<TextView>(R.id.textViewStressLevel).text = result.nivelEstres
        findViewById<TextView>(R.id.textViewRecommendation).text = result.recomendacion
        findViewById<TextView>(R.id.textViewDate).text = result.fecha
    }

    companion object {
        // Claves utilizadas para transportar informacion entre activities.
        const val EXTRA_SHOW_LATEST = "extra_show_latest"
        const val EXTRA_RESULT_ID = "extra_result_id"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_RECOMMENDATION = "extra_recommendation"
        const val EXTRA_DATE = "extra_date"
    }
}
