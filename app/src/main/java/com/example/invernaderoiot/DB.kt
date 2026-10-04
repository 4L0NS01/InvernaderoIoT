package com.example.invernaderoiot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "InvernaderoIoT.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)")


        db.execSQL("CREATE TABLE registros_sensores (id INTEGER PRIMARY KEY AUTOINCREMENT, valor TEXT, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")


        val passHash = cifrarSHA256("admin123")
        db.execSQL("INSERT INTO usuarios (username, password) VALUES ('admin', '$passHash')")


        db.execSQL("INSERT INTO registros_sensores (valor) VALUES ('Sistema en línea - Calibración inicial OK')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        db.execSQL("DROP TABLE IF EXISTS registros_sensores")
        onCreate(db)
    }

    fun validarLogin(user: String, pass: String): Boolean {
        val db = readableDatabase
        val passHash = cifrarSHA256(pass)
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE username = ? AND password = ?",
            arrayOf(user, passHash)
        )
        val valido = cursor.count > 0
        cursor.close()
        return valido
    }

    fun guardarLecturaSensor(valor: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("valor", valor)
        }
        db.insert("registros_sensores", null, values)
    }

    fun obtenerUltimoRegistro(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT valor, fecha FROM registros_sensores ORDER BY id DESC LIMIT 1", null)
        var resultado = "No hay registros guardados"
        if (cursor.moveToFirst()) {
            val valor = cursor.getString(0)
            val fecha = cursor.getString(1)
            resultado = "$valor (Fecha: $fecha)"
        }
        cursor.close()
        return resultado
    }

    private fun cifrarSHA256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}