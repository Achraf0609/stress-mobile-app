package com.example.appmovilstress.model

/*
 * Archivo de modelo combinado que une la informacion del resultado del test
 * con la recomendacion generada para ese mismo resultado.
 */
data class ResultadoConRecomendacion(
    val resultadoId: Long,
    val userId: Long,
    val puntuacion: Int,
    val nivelEstres: String,
    val fecha: String,
    val recomendacion: String
)
