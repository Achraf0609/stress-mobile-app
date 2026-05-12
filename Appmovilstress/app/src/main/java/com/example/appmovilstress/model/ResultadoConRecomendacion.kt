package com.example.appmovilstress.model

data class ResultadoConRecomendacion(
    val resultadoId: Long,
    val userId: Long,
    val puntuacion: Int,
    val nivelEstres: String,
    val fecha: String,
    val recomendacion: String
)
