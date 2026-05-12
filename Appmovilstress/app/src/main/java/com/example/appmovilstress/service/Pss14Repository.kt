package com.example.appmovilstress.service

import com.example.appmovilstress.model.Pregunta

object Pss14Repository {

    fun getQuestions(): List<Pregunta> = listOf(
        Pregunta(1, "En el último mes, ¿con qué frecuencia te has sentido afectado por algo que ocurrió inesperadamente?", false),
        Pregunta(2, "En el último mes, ¿con qué frecuencia te has sentido incapaz de controlar las cosas importantes de tu vida?", false),
        Pregunta(3, "En el último mes, ¿con qué frecuencia te has sentido nervioso o estresado?", false),
        Pregunta(4, "En el último mes, ¿con qué frecuencia has manejado con éxito los pequeños problemas irritantes de la vida?", true),
        Pregunta(5, "En el último mes, ¿con qué frecuencia has sentido que afrontabas eficazmente los cambios importantes que estaban ocurriendo en tu vida?", true),
        Pregunta(6, "En el último mes, ¿con qué frecuencia has sentido confianza en tu capacidad para manejar tus problemas personales?", true),
        Pregunta(7, "En el último mes, ¿con qué frecuencia has sentido que las cosas te iban bien?", true),
        Pregunta(8, "En el último mes, ¿con qué frecuencia has sentido que no podías afrontar todas las cosas que tenías que hacer?", false),
        Pregunta(9, "En el último mes, ¿con qué frecuencia has podido controlar las dificultades de tu vida?", true),
        Pregunta(10, "En el último mes, ¿con qué frecuencia has sentido que tenías el control de la situación?", true),
        Pregunta(11, "En el último mes, ¿con qué frecuencia has estado enfadado porque las cosas que te ocurrían estaban fuera de tu control?", false),
        Pregunta(12, "En el último mes, ¿con qué frecuencia has pensado en las cosas que te quedaban por hacer?", false),
        Pregunta(13, "En el último mes, ¿con qué frecuencia has podido controlar la forma en que empleabas tu tiempo?", true),
        Pregunta(14, "En el último mes, ¿con qué frecuencia has sentido que las dificultades se acumulaban tanto que no podías superarlas?", false)
    )

    val options = listOf(
        "0 = Nunca",
        "1 = Casi nunca",
        "2 = De vez en cuando",
        "3 = A menudo",
        "4 = Muy a menudo"
    )
}
