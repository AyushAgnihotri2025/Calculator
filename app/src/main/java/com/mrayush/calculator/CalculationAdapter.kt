import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrayush.calculator.R

class CalculationAdapter(private val calculations: List<String>) :
    RecyclerView.Adapter<CalculationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calculation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val calculation = calculations[calculations.size - 1 - position]
        holder.bind(calculation)
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
