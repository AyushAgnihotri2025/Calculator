package com.mrayush.calculator.screens

import CalculationAdapter
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrayush.calculator.CalculatorDatabaseHelper
import com.mrayush.calculator.R
import java.lang.Math.abs


class DataScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.data_screen)
        dataScreen()
    }
    fun countZeros(input: String): Int {
        return input.count { it == '0' }
    }

    // Mirror MainActivity's day/night preference so History matches the chosen
    // theme (localNightMode is per-activity, so it isn't inherited).
    private fun applySavedTheme() {
        val dayNight = getSharedPreferences("DayNight", MODE_PRIVATE)
            .getString("My_DayNight", "")
        delegate.localNightMode = if (dayNight == "yes") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }
    private fun dataScreen()
    {
        val dbHelper = CalculatorDatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM calculations", null)
        setupBackButton()

        val calculations = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val firstNumberIndex = cursor.getColumnIndex("first_number")
            val operationIndex = cursor.getColumnIndex("operation")
            val secondNumberIndex = cursor.getColumnIndex("second_number")
            val equalIndex = cursor.getColumnIndex("equals")
            val resultIndex = cursor.getColumnIndex("result")
            delete()
            // Check if all required columns are present in the current row
            if (firstNumberIndex >= 0 && operationIndex >= 0 && secondNumberIndex >= 0&&equalIndex>=0&&resultIndex>=0) {
                var firstNumber = cursor.getDouble(firstNumberIndex)
                var firstNumberInt=0
              var  secondNumberInt=0
                val operation = cursor.getString(operationIndex)
                val secondNumber = cursor.getDouble(secondNumberIndex)
                val equals = cursor.getString(equalIndex)
                val result = cursor.getDouble(resultIndex)
                var calculationString=""
if(countZeros(firstNumber.toString())!==firstNumber.toString().length-1)
{


                if(abs(firstNumber-firstNumber.toInt())<0.00000001)
{
    firstNumberInt=firstNumber.toInt()
    calculationString+=firstNumberInt.toString()

}
                else
                {
                    calculationString+=firstNumber.toString()

                }
}
                calculationString+=" "

                calculationString+=operation
                calculationString+=" "
                if(countZeros(secondNumber.toString())!==secondNumber.toString().length-1)
                {


                    if(abs(secondNumber-secondNumber.toInt())<0.00000001)
                    {
                        secondNumberInt=secondNumber.toInt()
                        calculationString+=secondNumberInt.toString()
                    }
                    else
                    {
                        calculationString+=secondNumber.toString()

                    }
               }
                calculationString+=" "
                calculationString+=equals
                calculationString+="  "

                if(abs(result-result.toInt())<0.00000001)
                {
                  val  result2=result.toInt()

                    calculationString+=result2.toString()
                }
                else
                {
                    calculationString+=result.toString()
                }
              if(firstNumber!==result&&secondNumber!==result){  calculations.add(calculationString)}
            }
        }
        cursor.close()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val adapter = CalculationAdapter(calculations)
        recyclerView.adapter = adapter
    }
    private fun goBack() {
        finish()
    }
    private fun delete()
    {
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
           deleteDatabase()
            goBack()
        }
    }

    private fun deleteDatabase()
    { val dbHelper = CalculatorDatabaseHelper(this)
        dbHelper.writableDatabase.execSQL("DELETE FROM calculations")

    }

    private fun setupBackButton() {
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            goBack()
        }
}}