package com.example.appmovilstress.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.appmovilstress.model.Recomendacion
import com.example.appmovilstress.model.ResultadoConRecomendacion
import com.example.appmovilstress.model.Usuario

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RESULTS (
                id_resultado INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL,
                puntuacion INTEGER NOT NULL,
                nivel_estres TEXT NOT NULL,
                fecha TEXT NOT NULL,
                FOREIGN KEY(id_usuario) REFERENCES $TABLE_USERS(id_usuario)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RECOMMENDATIONS (
                id_recomendacion INTEGER PRIMARY KEY AUTOINCREMENT,
                id_resultado INTEGER NOT NULL,
                texto TEXT NOT NULL,
                fecha_generacion TEXT NOT NULL,
                FOREIGN KEY(id_resultado) REFERENCES $TABLE_RESULTS(id_resultado)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECOMMENDATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESULTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(usuario: Usuario): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", usuario.nombre)
            put("email", usuario.email)
            put("password", usuario.password)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun login(email: String, password: String): Usuario? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id_usuario, nombre, email, password FROM $TABLE_USERS WHERE email = ? AND password = ?",
            arrayOf(email, password)
        )

        cursor.use {
            return if (it.moveToFirst()) {
                Usuario(
                    id = it.getLong(0),
                    nombre = it.getString(1),
                    email = it.getString(2),
                    password = it.getString(3)
                )
            } else {
                null
            }
        }
    }

    fun emailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id_usuario FROM $TABLE_USERS WHERE email = ?",
            arrayOf(email)
        )
        cursor.use {
            return it.count > 0
        }
    }

    fun saveResultWithRecommendation(
        userId: Long,
        puntuacion: Int,
        nivelEstres: String,
        fecha: String,
        textoRecomendacion: String
    ): Long {
        val db = writableDatabase
        db.beginTransaction()

        return try {
            val resultValues = ContentValues().apply {
                put("id_usuario", userId)
                put("puntuacion", puntuacion)
                put("nivel_estres", nivelEstres)
                put("fecha", fecha)
            }
            val resultId = db.insert(TABLE_RESULTS, null, resultValues)

            val recommendationValues = ContentValues().apply {
                put("id_resultado", resultId)
                put("texto", textoRecomendacion)
                put("fecha_generacion", fecha)
            }
            db.insert(TABLE_RECOMMENDATIONS, null, recommendationValues)

            db.setTransactionSuccessful()
            resultId
        } finally {
            db.endTransaction()
        }
    }

    fun getLastResult(userId: Long): ResultadoConRecomendacion? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT r.id_resultado, r.id_usuario, r.puntuacion, r.nivel_estres, r.fecha, rec.texto
            FROM $TABLE_RESULTS r
            INNER JOIN $TABLE_RECOMMENDATIONS rec ON rec.id_resultado = r.id_resultado
            WHERE r.id_usuario = ?
            ORDER BY r.id_resultado DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {
            return if (it.moveToFirst()) {
                ResultadoConRecomendacion(
                    resultadoId = it.getLong(0),
                    userId = it.getLong(1),
                    puntuacion = it.getInt(2),
                    nivelEstres = it.getString(3),
                    fecha = it.getString(4),
                    recomendacion = it.getString(5)
                )
            } else {
                null
            }
        }
    }

    fun getResults(userId: Long): List<ResultadoConRecomendacion> {
        val db = readableDatabase
        val resultList = mutableListOf<ResultadoConRecomendacion>()
        val cursor = db.rawQuery(
            """
            SELECT r.id_resultado, r.id_usuario, r.puntuacion, r.nivel_estres, r.fecha, rec.texto
            FROM $TABLE_RESULTS r
            INNER JOIN $TABLE_RECOMMENDATIONS rec ON rec.id_resultado = r.id_resultado
            WHERE r.id_usuario = ?
            ORDER BY r.fecha ASC, r.id_resultado ASC
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                resultList.add(
                    ResultadoConRecomendacion(
                        resultadoId = it.getLong(0),
                        userId = it.getLong(1),
                        puntuacion = it.getInt(2),
                        nivelEstres = it.getString(3),
                        fecha = it.getString(4),
                        recomendacion = it.getString(5)
                    )
                )
            }
        }
        return resultList
    }

    fun getRecommendations(userId: Long): List<Recomendacion> {
        val db = readableDatabase
        val list = mutableListOf<Recomendacion>()
        val cursor = db.rawQuery(
            """
            SELECT rec.id_recomendacion, rec.id_resultado, rec.texto, rec.fecha_generacion
            FROM $TABLE_RECOMMENDATIONS rec
            INNER JOIN $TABLE_RESULTS r ON r.id_resultado = rec.id_resultado
            WHERE r.id_usuario = ?
            ORDER BY rec.id_recomendacion DESC
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Recomendacion(
                        id = it.getLong(0),
                        resultadoId = it.getLong(1),
                        texto = it.getString(2),
                        fechaGeneracion = it.getString(3)
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "stress_monitor.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "usuarios"
        private const val TABLE_RESULTS = "resultados"
        private const val TABLE_RECOMMENDATIONS = "recomendaciones"
    }
}
