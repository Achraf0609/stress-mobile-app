package com.example.appmovilstress.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmovilstress.R
import com.google.android.material.appbar.MaterialToolbar

/*
 * Archivo base para las pantallas de la aplicacion.
 * Reutiliza la configuracion comun de la barra superior en todas las activities.
 */
open class BaseActivity : AppCompatActivity() {

    // Se mantiene el ciclo de vida estandar de AppCompatActivity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    // Configura el toolbar con titulo y boton de retroceso opcional.
    protected fun setupToolbar(toolbar: MaterialToolbar, title: String, showBackButton: Boolean = true) {
        toolbar.title = title
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBackButton)

        // Permite volver a la pantalla anterior al pulsar la flecha de navegacion.
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Aplica un color uniforme al texto del toolbar.
        toolbar.setTitleTextColor(getColor(R.color.white))
    }
}
