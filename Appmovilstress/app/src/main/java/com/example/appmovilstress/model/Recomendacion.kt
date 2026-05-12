package com.example.appmovilstress.model

data class Recomendacion(
    val id: Long = 0,
    val resultadoId: Long,
    val texto: String,
    val fechaGeneracion: String
)
