package com.example.appmovilstress.service

import com.example.appmovilstress.model.Pregunta

/*
 * Archivo que centraliza el contenido fijo del cuestionario PSS-14.
 * Aqui se define tanto el enunciado de cada pregunta como la escala de respuesta.
 */
object Pss14Repository {

    // Devuelve la lista completa de preguntas del cuestionario.
    fun getQuestions(): List<Pregunta> = listOf(
        Pregunta(1, "En el Ãºltimo mes, Â¿con quÃ© frecuencia te has sentido afectado por algo que ocurriÃ³ inesperadamente?", false),
        Pregunta(2, "En el Ãºltimo mes, Â¿con quÃ© frecuencia te has sentido incapaz de controlar las cosas importantes de tu vida?", false),
        Pregunta(3, "En el Ãºltimo mes, Â¿con quÃ© frecuencia te has sentido nervioso o estresado?", false),
        Pregunta(4, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has manejado con Ã©xito los pequeÃ±os problemas irritantes de la vida?", true),
        Pregunta(5, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido que afrontabas eficazmente los cambios importantes que estaban ocurriendo en tu vida?", true),
        Pregunta(6, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido confianza en tu capacidad para manejar tus problemas personales?", true),
        Pregunta(7, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido que las cosas te iban bien?", true),
        Pregunta(8, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido que no podÃ­as afrontar todas las cosas que tenÃ­as que hacer?", false),
        Pregunta(9, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has podido controlar las dificultades de tu vida?", true),
        Pregunta(10, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido que tenÃ­as el control de la situaciÃ³n?", true),
        Pregunta(11, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has estado enfadado porque las cosas que te ocurrÃ­an estaban fuera de tu control?", false),
        Pregunta(12, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has pensado en las cosas que te quedaban por hacer?", false),
        Pregunta(13, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has podido controlar la forma en que empleabas tu tiempo?", true),
        Pregunta(14, "En el Ãºltimo mes, Â¿con quÃ© frecuencia has sentido que las dificultades se acumulaban tanto que no podÃ­as superarlas?", false)
    )

    // Escala numerica y textual utilizada por todas las preguntas del cuestionario.
    val options = listOf(
        "0 = Nunca",
        "1 = Casi nunca",
        "2 = De vez en cuando",
        "3 = A menudo",
        "4 = Muy a menudo"
    )
}
