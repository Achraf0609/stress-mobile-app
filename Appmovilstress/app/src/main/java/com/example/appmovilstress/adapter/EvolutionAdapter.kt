package com.example.appmovilstress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appmovilstress.R
import com.example.appmovilstress.model.ResultadoConRecomendacion

/*
 * Archivo adaptador que transforma la lista de resultados en elementos visuales
 * para el RecyclerView mostrado en la pantalla de evolucion.
 */
class EvolutionAdapter(
    private val items: List<ResultadoConRecomendacion>
) : RecyclerView.Adapter<EvolutionAdapter.EvolutionViewHolder>() {

    // Crea la vista de cada fila del historial inflando el layout correspondiente.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvolutionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evolution, parent, false)
        return EvolutionViewHolder(view)
    }

    // Asocia cada resultado con su posicion visual dentro del RecyclerView.
    override fun onBindViewHolder(holder: EvolutionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Devuelve el numero total de resultados mostrados.
    override fun getItemCount(): Int = items.size

    // ViewHolder encargado de rellenar cada tarjeta del historial.
    class EvolutionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: ResultadoConRecomendacion) {
            itemView.findViewById<TextView>(R.id.textViewItemDate).text = item.fecha
            itemView.findViewById<TextView>(R.id.textViewItemScore).text = "Puntuacion: ${item.puntuacion}"

            // El nivel de estres cambia de color segun la categoria del resultado.
            itemView.findViewById<TextView>(R.id.textViewItemLevel).apply {
                text = item.nivelEstres
                val colorRes = when (item.nivelEstres) {
                    "Estr\u00e9s bajo" -> R.color.stress_low
                    "Estr\u00e9s moderado" -> R.color.stress_moderate
                    else -> R.color.stress_high
                }
                setTextColor(ContextCompat.getColor(context, colorRes))
            }
        }
    }
}
