package com.example.appmovilstress.model

/*
 * Archivo de modelo que representa a un usuario registrado en la aplicacion.
 * Se utiliza para guardar y recuperar la informacion basica de acceso.
 */
data class Usuario(
    val id: Long = 0,
    val nombre: String,
    val email: String,
    val password: String
)
