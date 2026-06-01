package com.example.appmovilstress.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmovilstress.R
import com.example.appmovilstress.adapter.EvolutionAdapter
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.ResultadoConRecomendacion
import com.example.appmovilstress.service.SessionManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo encargado de mostrar la evolucion temporal del estres del usuario.
 * Combina una grafica de linea con un historial detallado de resultados.
 */
class EvolutionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evolution)

        // Configura el toolbar de la pantalla de evolucion.
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.evolution_title))

        // Enlaza los componentes visuales necesarios para la grafica y el historial.
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEvolution)
        val emptyState = findViewById<TextView>(R.id.textViewEmptyState)
        val chartText = findViewById<TextView>(R.id.textViewChartSummary)
        val testsCount = findViewById<TextView>(R.id.textViewTestsCount)
        val latestResult = findViewById<TextView>(R.id.textViewLatestResult)
        val lineChart = findViewById<LineChart>(R.id.lineChartStress)

        // Recuperacion del historial de resultados del usuario autenticado desde la base de datos local.
        val results = SQLiteHelper(this).getResults(SessionManager(this).getUserId())

        // Si no hay resultados, se muestra un estado vacio y se ocultan grafica e historial.
        if (results.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            lineChart.visibility = View.GONE
            chartText.text = getString(R.string.no_data)
            testsCount.text = "0"
            latestResult.text = "-"
            return
        }

        // Si hay resultados, se inicializan la grafica y la lista de historial.
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        lineChart.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = EvolutionAdapter(results)

        // Preparacion de la informacion del historial para mostrarla en la pantalla de evolucion.
        configureChart(lineChart, results)

        // Muestra un resumen rapido con el numero de tests y el ultimo resultado registrado.
        val last = results.last()
        testsCount.text = results.size.toString()
        latestResult.text = last.puntuacion.toString()
        chartText.text =
            "Historial registrado: ${results.size} tests. Ultimo valor: ${last.puntuacion} puntos (${last.nivelEstres})."
    }

    // Configura la grafica de linea y le asigna los datos del historial.
    private fun configureChart(lineChart: LineChart, results: List<ResultadoConRecomendacion>) {
        // Transformacion del historial de resultados en puntos y etiquetas que puede representar la grafica.
        val entries = results.mapIndexed { index, result ->
            Entry(index.toFloat(), result.puntuacion.toFloat())
        }
        val labels = results.map { shortenDate(it.fecha) }

        // Define el estilo visual de la linea que representa la puntuacion en el tiempo.
        val dataSet = LineDataSet(entries, "Puntuacion PSS-14").apply {
            color = getColor(R.color.primary)
            valueTextColor = getColor(R.color.primary_dark)
            lineWidth = 3f
            setCircleColor(getColor(R.color.primary_dark))
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 11f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = getColor(R.color.accent)
            highLightColor = getColor(R.color.stress_moderate)
        }

        // Personaliza ejes, leyenda y comportamiento general de la grafica.
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            setNoDataText(getString(R.string.no_data))
            axisRight.isEnabled = false
            setExtraOffsets(8f, 8f, 8f, 8f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = getColor(R.color.text_secondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        return labels.getOrNull(index).orEmpty()
                    }
                }
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 56f
                granularity = 7f
                textColor = getColor(R.color.text_secondary)
                gridColor = getColor(R.color.divider)
            }

            data = LineData(dataSet)
            animateX(900)
            invalidate()
        }
    }

    // Recorta la fecha para mostrar solo la parte principal en el eje X.
    private fun shortenDate(fullDate: String): String {
        return fullDate.substringBefore(" ")
    }
}
