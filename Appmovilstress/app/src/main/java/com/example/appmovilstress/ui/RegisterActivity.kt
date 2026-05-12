package com.example.appmovilstress.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.appmovilstress.R
import com.example.appmovilstress.database.SQLiteHelper
import com.example.appmovilstress.model.Usuario
import com.google.android.material.appbar.MaterialToolbar

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), getString(R.string.register))

        val database = SQLiteHelper(this)
        val nameEditText = findViewById<EditText>(R.id.editTextName)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)

        findViewById<Button>(R.id.buttonCreateAccount).setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (database.emailExists(email)) {
                Toast.makeText(this, "Ya existe un usuario con ese email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val insertedId = database.registerUser(Usuario(nombre = name, email = email, password = password))
            if (insertedId > 0) {
                Toast.makeText(this, "Usuario registrado correctamente.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "No se pudo registrar el usuario.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
