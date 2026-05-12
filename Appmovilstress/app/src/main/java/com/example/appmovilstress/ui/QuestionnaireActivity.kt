package com.example.appmovilstress.ui

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.Pregunta
import com.example.appmovilstress.service.AIRecommendationService
import com.example.appmovilstress.service.Pss14Repository
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuestionnaireActivity : BaseActivity() {

    private val questionGroups = mutableListOf<Pair<Pregunta, RadioGroup>>()
    private lateinit var finishButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questionnaire)

        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.questionnaire_title))

        val container = findViewById<LinearLayout>(R.id.questionContainer)
        val questions = Pss14Repository.getQuestions()
        createQuestionViews(container, questions)

        finishButton = findViewById(R.id.buttonFinishQuestionnaire)
        finishButton.setOnClickListener {
            saveQuestionnaire()
        }
    }

    private fun createQuestionViews(container: LinearLayout, questions: List<Pregunta>) {
        val margin = resources.getDimensionPixelSize(R.dimen.spacing_medium)

        questions.forEach { pregunta ->
            val title = TextView(this).apply {
                text = "${pregunta.id}. ${pregunta.texto}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(0, margin, 0, margin / 2)
            }

            val group = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = margin
                }
                setBackgroundResource(R.drawable.bg_question_card)
                setPadding(margin, margin, margin, margin)
            }

            Pss14Repository.options.forEachIndexed { index, label ->
                val radioButton = RadioButton(this).apply {
                    id = ViewGroup.generateViewId()
                    text = label
                    tag = index
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                group.addView(radioButton)
            }

            questionGroups.add(pregunta to group)
            container.addView(title)
            container.addView(group)
        }
    }

    private fun saveQuestionnaire() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        if (userId == -1L) {
            Toast.makeText(this, "La sesion no es valida.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        var total = 0

        for ((pregunta, group) in questionGroups) {
            if (group.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Responde todas las preguntas antes de finalizar.", Toast.LENGTH_LONG).show()
                return
            }

            val selected = group.findViewById<RadioButton>(group.checkedRadioButtonId)
            val responseValue = selected.tag as Int
            total += if (pregunta.esInversa) 4 - responseValue else responseValue
        }

        val nivel = classifyStress(total)
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        setLoadingState(true)

        lifecycleScope.launch {
            val recommendationResult = withContext(Dispatchers.IO) {
                AIRecommendationService(getString(R.string.gemini_api_key))
                    .generarRecomendacion(total, nivel)
            }
            recommendationResult.aviso?.let {
                Toast.makeText(this@QuestionnaireActivity, it, Toast.LENGTH_LONG).show()
            }

            val database = SQLiteHelper(this@QuestionnaireActivity)
            val resultId = database.saveResultWithRecommendation(
                userId,
                total,
                nivel,
                fecha,
                recommendationResult.texto
            )

            val intent = Intent(this@QuestionnaireActivity, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_RESULT_ID, resultId)
                putExtra(ResultActivity.EXTRA_SCORE, total)
                putExtra(ResultActivity.EXTRA_LEVEL, nivel)
                putExtra(ResultActivity.EXTRA_RECOMMENDATION, recommendationResult.texto)
                putExtra(ResultActivity.EXTRA_DATE, fecha)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun classifyStress(score: Int): String {
        return when (score) {
            in 0..18 -> "Estr\u00e9s bajo"
            in 19..37 -> "Estr\u00e9s moderado"
            else -> "Estr\u00e9s alto"
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        finishButton.isEnabled = !isLoading
        finishButton.text = if (isLoading) {
            "Generando recomendacion..."
        } else {
            getString(R.string.finish_test)
        }
    }
}
