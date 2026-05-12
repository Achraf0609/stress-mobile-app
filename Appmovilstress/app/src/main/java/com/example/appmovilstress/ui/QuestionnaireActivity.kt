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
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.Pregunta
import com.example.appmovilstress.service.AIRecommendationService
import com.example.appmovilstress.service.Pss14Repository
import com.example.appmovilstress.service.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuestionnaireActivity : BaseActivity() {

    private val questionGroups = mutableListOf<Pair<Pregunta, RadioGroup>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questionnaire)

        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.questionnaire_title))

        val container = findViewById<LinearLayout>(R.id.questionContainer)
        val questions = Pss14Repository.getQuestions()
        createQuestionViews(container, questions)

        findViewById<Button>(R.id.buttonFinishQuestionnaire).setOnClickListener {
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
            Toast.makeText(this, "La sesión no es válida.", Toast.LENGTH_SHORT).show()
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
        val recommendation = AIRecommendationService().generarRecomendacion(nivel)

        val database = SQLiteHelper(this)
        val resultId = database.saveResultWithRecommendation(userId, total, nivel, fecha, recommendation)

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_RESULT_ID, resultId)
            putExtra(ResultActivity.EXTRA_SCORE, total)
            putExtra(ResultActivity.EXTRA_LEVEL, nivel)
            putExtra(ResultActivity.EXTRA_RECOMMENDATION, recommendation)
            putExtra(ResultActivity.EXTRA_DATE, fecha)
        }
        startActivity(intent)
        finish()
    }

    private fun classifyStress(score: Int): String {
        return when (score) {
            in 0..18 -> "Estrés bajo"
            in 19..37 -> "Estrés moderado"
            else -> "Estrés alto"
        }
    }
}
