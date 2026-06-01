package com.example.appmovilstress.model

/*
 * Archivo de modelo que representa una recomendacion guardada en la base de datos.
 * Cada recomendacion queda asociada a un resultado concreto del cuestionario.
 */
data class Recomendacion(
    val id: Long = 0,
    val resultadoId: Long,
    val texto: String,
    val fechaGeneracion: String
)
