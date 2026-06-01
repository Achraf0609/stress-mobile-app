package com.example.appmovilstress.model

/*
 * Archivo de modelo que representa una pregunta del cuestionario PSS-14.
 * El campo esInversa indica si la puntuacion debe invertirse al calcular el total.
 */
data class Pregunta(
    val id: Int,
    val texto: String,
    val esInversa: Boolean
)
