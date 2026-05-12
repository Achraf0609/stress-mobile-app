package com.example.appmovilstress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appmovilstress.R
import com.example.appmovilstress.model.ResultadoConRecomendacion

class EvolutionAdapter(
    private val items: List<ResultadoConRecomendacion>
) : RecyclerView.Adapter<EvolutionAdapter.EvolutionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvolutionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evolution, parent, false)
        return EvolutionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvolutionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EvolutionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: ResultadoConRecomendacion) {
            itemView.findViewById<TextView>(R.id.textViewItemDate).text = item.fecha
            itemView.findViewById<TextView>(R.id.textViewItemScore).text = "Puntuación: ${item.puntuacion}"
            itemView.findViewById<TextView>(R.id.textViewItemLevel).apply {
                text = item.nivelEstres
                val colorRes = when (item.nivelEstres) {
                    "Estrés bajo" -> R.color.stress_low
                    "Estrés moderado" -> R.color.stress_moderate
                    else -> R.color.stress_high
                }
                setTextColor(ContextCompat.getColor(context, colorRes))
            }
        }
    }
}
