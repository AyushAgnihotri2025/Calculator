package com.mrayush.calculator

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CalculatorDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "calculator.db", null, 1) {
    // Implement onCreate and onUpgrade methods
    override fun onCreate(db: SQLiteDatabase?) {
        // Create a table for calculations
        db?.execSQL("CREATE TABLE calculations (id INTEGER PRIMARY KEY AUTOINCREMENT, first_number REAL, operation TEXT, second_number REAL,equals TEXT,result REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}