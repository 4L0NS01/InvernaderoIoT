package com.example.invernaderoiot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val campoUsuario = findViewById<EditText>(R.id.campoUsuario)
        val campoClave = findViewById<EditText>(R.id.campoClave)
        val botonEntrar = findViewById<Button>(R.id.botonEntrar)

        botonEntrar.setOnClickListener {
            val user = campoUsuario.text.toString().trim()
            val pass = campoClave.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Debe ingresar usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.validarLogin(user, pass)) {
                Toast.makeText(this, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Acceso denegado: Credenciales inválidas (ISO 27400)", Toast.LENGTH_LONG).show()
            }
        }
    }
}