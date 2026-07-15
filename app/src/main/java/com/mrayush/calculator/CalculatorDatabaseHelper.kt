package com.mrayush.calculator

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CalculatorDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
            CREATE TABLE calculations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                first_number REAL,
                operation TEXT NOT NULL,
                second_number REAL,
                equals TEXT NOT NULL,
                result REAL,
                first_text TEXT,
                second_text TEXT,
                result_text TEXT,
                angle_unit TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE calculations ADD COLUMN first_text TEXT")
            db?.execSQL("ALTER TABLE calculations ADD COLUMN second_text TEXT")
            db?.execSQL("ALTER TABLE calculations ADD COLUMN result_text TEXT")
            db?.execSQL(
                """
                UPDATE calculations
                SET first_text = CASE
                        WHEN first_number IS NULL THEN '' ELSE CAST(first_number AS TEXT) END,
                    second_text = CASE
                        WHEN second_number IS NULL THEN '' ELSE CAST(second_number AS TEXT) END,
                    result_text = CASE
                        WHEN result IS NULL THEN '' ELSE CAST(result AS TEXT) END
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE calculations ADD COLUMN angle_unit TEXT")
            db?.execSQL(
                """
                UPDATE calculations
                SET angle_unit = 'DEGREES'
                WHERE operation IN ('sin', 'cos', 'tan')
                """.trimIndent()
            )
        }
    }

    fun insertCalculation(
        firstText: String,
        operation: String,
        secondText: String,
        resultText: String,
        angleUnit: AngleUnit? = null
    ) {
        val values = ContentValues().apply {
            put("first_text", firstText)
            put("operation", operation)
            put("second_text", secondText)
            put("equals", "=")
            put("result_text", resultText)
            angleUnit?.let { put("angle_unit", it.name) }
            firstText.toFiniteDoubleOrNull()?.let { put("first_number", it) }
            secondText.toFiniteDoubleOrNull()?.let { put("second_number", it) }
            resultText.toFiniteDoubleOrNull()?.let { put("result", it) }
        }
        writableDatabase.insertOrThrow("calculations", null, values)
    }

    private fun String.toFiniteDoubleOrNull(): Double? =
        toDoubleOrNull()?.takeIf { it.isFinite() }

    companion object {
        private const val DATABASE_NAME = "calculator.db"
        private const val DATABASE_VERSION = 3
    }
}