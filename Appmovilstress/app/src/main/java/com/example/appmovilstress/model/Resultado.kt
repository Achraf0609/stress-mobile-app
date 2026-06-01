package com.example.appmovilstress.model

/*
 * Archivo de modelo que representa un resultado individual del cuestionario PSS-14.
 * Contiene la puntuacion obtenida, el nivel de estres calculado y la fecha del test.
 */
data class Resultado(
    val id: Long = 0,
    val userId: Long,
    val puntuacion: Int,
    val nivelEstres: String,
    val fecha: String
)
