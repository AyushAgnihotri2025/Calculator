package com.mrayush.calculator.screens

import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrayush.calculator.CalculationAdapter
import com.mrayush.calculator.CalculatorDatabaseHelper
import com.mrayush.calculator.R
import java.math.BigDecimal

class DataScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.data_screen)
        setupButtons()
        showHistory()
    }

    private fun applySavedTheme() {
        val dayNight = getSharedPreferences("DayNight", MODE_PRIVATE)
            .getString("My_DayNight", "")
        delegate.localNightMode = if (dayNight == "yes") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun showHistory() {
        val calculations = mutableListOf<String>()
        CalculatorDatabaseHelper(this).use { helper ->
            helper.readableDatabase.rawQuery(
                "SELECT * FROM calculations ORDER BY id DESC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    formatHistoryRow(cursor)?.let(calculations::add)
                }
            }
        }

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@DataScreen)
            adapter = CalculationAdapter(calculations)
        }
    }

    private fun formatHistoryRow(cursor: Cursor): String? {
        val operation = cursor.stringValue("operation") ?: return null
        val result = cursor.exactOrLegacyText("result_text", "result") ?: return null
        val first = cursor.exactOrLegacyText("first_text", "first_number").orEmpty()
        val second = cursor.exactOrLegacyText("second_text", "second_number").orEmpty()

        val expression = when (operation) {
            "!", "^-1" -> "$first$operation"
            "log", "ln", "√", "sin", "cos", "tan", "e^" -> "$operation$first"
            else -> "$first $operation $second"
        }
        return "$expression = $result"
    }

    private fun Cursor.exactOrLegacyText(textColumn: String, numberColumn: String): String? {
        stringValue(textColumn)?.takeIf(String::isNotBlank)?.let { return it }
        val index = getColumnIndex(numberColumn)
        if (index < 0 || isNull(index)) return null
        return BigDecimal.valueOf(getDouble(index)).stripTrailingZeros().toPlainString()
    }

    private fun Cursor.stringValue(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_all) { _, _ ->
                    CalculatorDatabaseHelper(this).use { helper ->
                        helper.writableDatabase.delete("calculations", null, null)
                    }
                    showHistory()
                }
                .show()
        }
    }
}