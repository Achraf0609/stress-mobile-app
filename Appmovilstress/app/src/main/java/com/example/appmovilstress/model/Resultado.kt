package com.example.appmovilstress.model

data class Resultado(
    val id: Long = 0,
    val userId: Long,
    val puntuacion: Int,
    val nivelEstres: String,
    val fecha: String
)
