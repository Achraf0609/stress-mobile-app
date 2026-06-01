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

/*
 * Archivo encargado de mostrar el cuestionario PSS-14, recoger las respuestas
 * y calcular el nivel de estres antes de generar la recomendacion personalizada.
 */
class QuestionnaireActivity : BaseActivity() {

    // Relaciona cada pregunta con su grupo de opciones para poder leer la respuesta seleccionada.
    private val questionGroups = mutableListOf<Pair<Pregunta, RadioGroup>>()

    // Referencia al boton final para poder bloquearlo mientras se genera la recomendacion.
    private lateinit var finishButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questionnaire)

        // Configura el toolbar del cuestionario.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.questionnaire_title))

        // Recupera las preguntas del repositorio y construye la interfaz de forma dinamica.
        val container = findViewById<LinearLayout>(R.id.questionContainer)
        val questions = Pss14Repository.getQuestions()
        createQuestionViews(container, questions)

        // Configura el boton que finaliza el cuestionario y dispara el calculo.
        finishButton = findViewById(R.id.buttonFinishQuestionnaire)
        finishButton.setOnClickListener {
            saveQuestionnaire()
        }
    }

    // Genera visualmente cada pregunta y sus opciones de respuesta.
    private fun createQuestionViews(container: LinearLayout, questions: List<Pregunta>) {
        val margin = resources.getDimensionPixelSize(R.dimen.spacing_medium)

        // Recorrido de la lista de preguntas del cuestionario PSS-14 para construir la interfaz dinamicamente.
        questions.forEach { pregunta ->
            val title = TextView(this).apply {
                text = "${pregunta.id}. ${pregunta.texto}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(0, margin, 0, margin / 2)
            }

            // Cada pregunta utiliza un RadioGroup para obligar a seleccionar solo una opcion.
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

            // Creacion de las opciones de respuesta y asociacion del valor numerico mediante la propiedad tag.
            Pss14Repository.options.forEachIndexed { index, label ->
                val radioButton = RadioButton(this).apply {
                    id = ViewGroup.generateViewId()
                    text = label
                    tag = index
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                group.addView(radioButton)
            }

            // Guarda la referencia al grupo de respuestas y lo anade al contenedor visual.
            questionGroups.add(pregunta to group)
            container.addView(title)
            container.addView(group)
        }
    }

    // Recoge las respuestas, calcula la puntuacion y lanza el proceso de guardado.
    private fun saveQuestionnaire() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // Evita guardar resultados si no existe una sesion valida.
        if (userId == -1L) {
            Toast.makeText(this, "La sesion no es valida.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        var total = 0

        // Recuperacion de la respuesta seleccionada en cada pregunta y transformacion a su valor numerico.
        for ((pregunta, group) in questionGroups) {
            if (group.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Responde todas las preguntas antes de finalizar.", Toast.LENGTH_LONG).show()
                return
            }

            val selected = group.findViewById<RadioButton>(group.checkedRadioButtonId)
            val responseValue = selected.tag as Int

            // Calculo de la puntuacion total del cuestionario.
            // Si la pregunta es un item positivo, la puntuacion se invierte aplicando 4 - respuesta.
            // En el resto de preguntas se suma directamente el valor marcado por el usuario.
            total += if (pregunta.esInversa) 4 - responseValue else responseValue
        }

        // La variable total contiene la puntuacion final del PSS-14 tras sumar todos los items, incluidos los invertidos.
        val nivel = classifyStress(total)
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // Desactiva el boton para evitar que el usuario envie varias veces el cuestionario.
        setLoadingState(true)

        val database = SQLiteHelper(this)
        val recomendacionesPrevias = database.getRecentRecommendationTexts(userId)

        // Ejecuta la llamada a Gemini y el guardado de datos fuera del hilo principal.
        lifecycleScope.launch {
            val recommendationResult = withContext(Dispatchers.IO) {
                AIRecommendationService(getString(R.string.gemini_api_key))
                    .generarRecomendacion(total, nivel, recomendacionesPrevias)
            }

            // Si Gemini devuelve alguna advertencia, se informa al usuario.
            recommendationResult.aviso?.let {
                Toast.makeText(this@QuestionnaireActivity, it, Toast.LENGTH_LONG).show()
            }

            // Guarda el resultado del cuestionario y la recomendacion generada en la base de datos local.
            val resultId = database.saveResultWithRecommendation(
                userId,
                total,
                nivel,
                fecha,
                recommendationResult.texto
            )

            // Navega a la pantalla de resultado mostrando la informacion recien calculada.
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

    // Convierte la puntuacion total del cuestionario en una categoria interpretable.
    private fun classifyStress(score: Int): String {
        return when (score) {
            in 0..18 -> "Estr\u00e9s bajo"
            in 19..37 -> "Estr\u00e9s moderado"
            else -> "Estr\u00e9s alto"
        }
    }

    // Cambia el estado visual del boton mientras se genera la recomendacion.
    private fun setLoadingState(isLoading: Boolean) {
        finishButton.isEnabled = !isLoading
        finishButton.text = if (isLoading) {
            "Generando recomendacion..."
        } else {
            getString(R.string.finish_test)
        }
    }
}
