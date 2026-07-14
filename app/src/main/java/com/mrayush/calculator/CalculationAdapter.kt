package com.mrayush.calculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalculationAdapter(private val calculations: List<String>) :
    RecyclerView.Adapter<CalculationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calculation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(calculations[position])
    }

    override fun getItemCount(): Int {
        return calculations.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val calculationTextView: TextView = itemView.findViewById(R.id.textView4)

        fun bind(calculation: String) {
            calculationTextView.text = calculation
        }
    }
}
