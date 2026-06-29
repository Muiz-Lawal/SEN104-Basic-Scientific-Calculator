package com.abdulmuizlawal.calculator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

class StatisticsActivity : AppCompatActivity() {

    private lateinit var inputEdit: TextInputEditText
    private lateinit var resultCard: MaterialCardView
    private lateinit var df: DecimalFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.stat_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Use '0' instead of '#' to ensure 0 is displayed as "0" not ""
        df = DecimalFormat("0.########")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        inputEdit = findViewById(R.id.edit_stat_input)
        resultCard = findViewById(R.id.result_card)

        findViewById<MaterialButton>(R.id.btn_calculate_stat).setOnClickListener {
            calculateStatistics()
        }

        findViewById<MaterialButton>(R.id.btn_clear_stat).setOnClickListener {
            inputEdit.text?.clear()
            resultCard.visibility = View.GONE
        }

        findViewById<MaterialButton>(R.id.btn_example_stat).setOnClickListener {
            inputEdit.setText("12, 15, 12, 18, 20, 22, 15, 14, 12, 25")
            calculateStatistics()
        }
    }

    private fun calculateStatistics() {
        val input = inputEdit.text.toString()
        if (input.isEmpty()) return

        val numbers = input.split(Regex("[,\\s\\n]+"))
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toDoubleOrNull() }

        if (numbers.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_invalid_data), Toast.LENGTH_SHORT).show()
            return
        }

        val count = numbers.size
        val sum = numbers.sum()
        val mean = sum / count
        
        val sorted = numbers.sorted()
        
        val median = calculateMedian(sorted)
        val mode = calculateMode(numbers)
        val variance = numbers.sumOf { (it - mean).pow(2.0) } / count
        val stdDev = sqrt(variance)
        val min = sorted.first()
        val max = sorted.last()
        val range = max - min
        val q1 = calculateQuartile(sorted, 0.25)
        val q3 = calculateQuartile(sorted, 0.75)
        val iqr = q3 - q1

        displayResults(mean, median, mode, stdDev, variance, min, max, range, sum, count, q1, q3, iqr)
    }

    private fun calculateMedian(sorted: List<Double>): Double {
        val n = sorted.size
        return if (n % 2 == 0) {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        } else {
            sorted[n / 2]
        }
    }

    private fun calculateMode(numbers: List<Double>): String {
        val counts = numbers.groupingBy { it }.eachCount()
        val maxCount = counts.maxByOrNull { it.value }?.value ?: 0
        if (maxCount <= 1 && numbers.size > 1) return "None"
        
        val modes = counts.filter { it.value == maxCount }.keys
        return if (modes.size > 3) "Multi" else modes.joinToString(", ") { df.format(it) }
    }

    private fun calculateQuartile(sorted: List<Double>, percentile: Double): Double {
        val index = percentile * (sorted.size - 1)
        val lower = index.toInt()
        val upper = lower + 1
        val weight = index - lower
        return if (upper >= sorted.size) sorted[lower]
        else sorted[lower] * (1 - weight) + sorted[upper] * weight
    }

    private fun displayResults(
        mean: Double, med: Double, mode: String, std: Double, v: Double,
        min: Double, max: Double, r: Double, s: Double, c: Int,
        q1: Double, q3: Double, iqr: Double
    ) {
        setupRow(R.id.row_mean, getString(R.string.mean), df.format(mean))
        setupRow(R.id.row_median, getString(R.string.median), df.format(med))
        setupRow(R.id.row_mode, getString(R.string.mode), mode)
        setupRow(R.id.row_std_dev, getString(R.string.std_dev), df.format(std))
        setupRow(R.id.row_variance, getString(R.string.variance), df.format(v))
        setupRow(R.id.row_range, getString(R.string.range), df.format(r))
        setupRow(R.id.row_q1, getString(R.string.q1), df.format(q1))
        setupRow(R.id.row_q3, getString(R.string.q3), df.format(q3))
        setupRow(R.id.row_iqr, getString(R.string.iqr), df.format(iqr))
        setupRow(R.id.row_min, getString(R.string.min_label), df.format(min))
        setupRow(R.id.row_max, getString(R.string.max_label), df.format(max))
        setupRow(R.id.row_sum, getString(R.string.sum_label), df.format(s))
        setupRow(R.id.row_count, getString(R.string.count_label), c.toString())

        resultCard.visibility = View.VISIBLE

        // Save to History
        val historyResult = "Mean: ${df.format(mean)} | StdDev: ${df.format(std)} | Count: $c"
        HistoryHelper.saveEntry(this, getString(R.string.statistics_title), historyResult)
    }

    private fun setupRow(viewId: Int, label: String, value: String) {
        val view = findViewById<View>(viewId)
        view.findViewById<TextView>(R.id.stat_label).text = label
        view.findViewById<TextView>(R.id.stat_value).text = value
    }
}
