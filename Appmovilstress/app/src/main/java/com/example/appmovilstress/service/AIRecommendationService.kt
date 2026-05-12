package com.example.appmovilstress.service

class AIRecommendationService {

    fun generarRecomendacion(nivelEstres: String): String {
        return when (nivelEstres) {
            "Estrés bajo" -> "Tu nivel de estrés es bajo. Mantén tus hábitos saludables, duerme bien, realiza actividad física y reserva tiempo para actividades que te aporten bienestar."
            "Estrés moderado" -> "Tu nivel de estrés es moderado. Intenta introducir pausas activas, ejercicios de respiración y una mejor organización del tiempo para reducir la carga diaria."
            else -> "Tu nivel de estrés es alto. Prioriza el descanso, utiliza técnicas de relajación como respiración guiada o mindfulness y valora consultar con un profesional si esta situación persiste."
        }
    }
}
