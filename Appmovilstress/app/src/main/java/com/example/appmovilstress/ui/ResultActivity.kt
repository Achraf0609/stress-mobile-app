package com.example.appmovilstress.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.ResultadoConRecomendacion
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar

class ResultActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.result_title))

        val emptyState = findViewById<TextView>(R.id.textViewEmptyState)
        val contentGroup = findViewById<View>(R.id.resultContent)

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

    private fun bindResult(result: ResultadoConRecomendacion) {
        findViewById<TextView>(R.id.textViewScore).text = result.puntuacion.toString()
        findViewById<TextView>(R.id.textViewStressLevel).text = result.nivelEstres
        findViewById<TextView>(R.id.textViewRecommendation).text = result.recomendacion
        findViewById<TextView>(R.id.textViewDate).text = result.fecha
    }

    companion object {
        const val EXTRA_SHOW_LATEST = "extra_show_latest"
        const val EXTRA_RESULT_ID = "extra_result_id"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_RECOMMENDATION = "extra_recommendation"
        const val EXTRA_DATE = "extra_date"
    }
}
