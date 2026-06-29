package com.abdulmuizlawal.calculator

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

import java.util.Locale

class MatrixActivity : AppCompatActivity() {

    private lateinit var gridA: GridLayout
    private lateinit var gridB: GridLayout
    private lateinit var resultLabel: TextView
    private lateinit var resultDisplay: TextView

    private var rows = 3
    private var cols = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_matrix)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.matrix_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        gridA = findViewById(R.id.grid_matrix_a)
        gridB = findViewById(R.id.grid_matrix_b)
        resultLabel = findViewById(R.id.result_matrix_label)
        resultDisplay = findViewById(R.id.display_matrix_result)

        setupGrids()

        findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_dimensions).addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_2x2 -> {
                        rows = 2
                        cols = 2
                    }
                    R.id.btn_3x3 -> {
                        rows = 3
                        cols = 3
                    }
                }
                setupGrids()
            }
        }

        findViewById<MaterialButton>(R.id.btn_add_matrix).setOnClickListener { calculate("+") }
        findViewById<MaterialButton>(R.id.btn_sub_matrix).setOnClickListener { calculate("-") }
        findViewById<MaterialButton>(R.id.btn_mul_matrix).setOnClickListener { calculate("*") }
        findViewById<MaterialButton>(R.id.btn_det_a).setOnClickListener { calculateDet() }
        findViewById<MaterialButton>(R.id.btn_transpose_a).setOnClickListener { calculateTranspose() }
        findViewById<MaterialButton>(R.id.btn_inv_a).setOnClickListener { calculateInverse() }
        findViewById<MaterialButton>(R.id.btn_clear_matrices).setOnClickListener { clearMatrices() }
    }

    private fun setupGrids() {
        gridA.removeAllViews()
        gridB.removeAllViews()
        gridA.columnCount = cols
        gridA.rowCount = rows
        gridB.columnCount = cols
        gridB.rowCount = rows
        for (i in 0 until rows * cols) {
            gridA.addView(createMatrixEditText())
            gridB.addView(createMatrixEditText())
        }
        resultLabel.visibility = View.GONE
        resultDisplay.visibility = View.GONE
    }

    private fun createMatrixEditText(): EditText {
        val widthInDp = 64
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthInDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        return EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            width = px
            hint = "0"
            gravity = android.view.Gravity.CENTER
            setTextColor(getColor(R.color.text_display_color))
            setHintTextColor(getColor(R.color.text_secondary_color))
        }
    }

    private fun getMatrix(grid: GridLayout, validate: Boolean = false): Array<DoubleArray>? {
        val matrix = Array(rows) { DoubleArray(cols) }
        for (i in 0 until grid.childCount) {
            val editText = grid.getChildAt(i) as EditText
            val text = editText.text.toString()
            if (validate && text.isEmpty()) {
                return null
            }
            val value = text.toDoubleOrNull() ?: 0.0
            matrix[i / cols][i % cols] = value
        }
        return matrix
    }

    private fun calculate(op: String) {
        val a = getMatrix(gridA, true)
        val b = getMatrix(gridB, true)
        
        if (a == null || b == null) {
            showError(getString(R.string.error_incomplete_matrix))
            return
        }

        val result = when (op) {
            "+" -> addMatrices(a, b)
            "-" -> subtractMatrices(a, b)
            "*" -> multiplyMatrices(a, b)
            else -> null
        }

        if (result != null) {
            displayResult(result)
        }
    }

    private fun addMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val res = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                res[i][j] = a[i][j] + b[i][j]
            }
        }
        return res
    }

    private fun subtractMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val res = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                res[i][j] = a[i][j] - b[i][j]
            }
        }
        return res
    }

    private fun multiplyMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val res = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += a[i][k] * b[k][j]
                }
                res[i][j] = sum
            }
        }
        return res
    }

    private fun calculateDet() {
        val a = getMatrix(gridA, true)
        if (a == null) {
            showError(getString(R.string.error_incomplete_matrix))
            return
        }
        val det = determinant(a)
        resultLabel.visibility = View.VISIBLE
        resultDisplay.visibility = View.VISIBLE
        resultDisplay.text = getString(R.string.det_result, String.format(Locale.US, "%.2f", det))
    }

    private fun determinant(m: Array<DoubleArray>): Double {
        return if (rows == 2) {
            m[0][0] * m[1][1] - m[0][1] * m[1][0]
        } else {
            m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
                    m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                    m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
        }
    }

    private fun calculateInverse() {
        val a = getMatrix(gridA, true)
        if (a == null) {
            showError(getString(R.string.error_incomplete_matrix))
            return
        }
        val det = determinant(a)
        if (det == 0.0) {
            showError(getString(R.string.error_det_zero))
            return
        }

        val inv = if (rows == 2) {
            arrayOf(
                doubleArrayOf(a[1][1] / det, -a[0][1] / det),
                doubleArrayOf(-a[1][0] / det, a[0][0] / det)
            )
        } else {
            val adj = Array(3) { DoubleArray(3) }
            adj[0][0] = (a[1][1] * a[2][2] - a[1][2] * a[2][1])
            adj[1][0] = -(a[1][0] * a[2][2] - a[1][2] * a[2][0])
            adj[2][0] = (a[1][0] * a[2][1] - a[1][1] * a[2][0])
            adj[0][1] = -(a[0][1] * a[2][2] - a[0][2] * a[2][1])
            adj[1][1] = (a[0][0] * a[2][2] - a[0][2] * a[2][0])
            adj[2][1] = -(a[0][0] * a[2][1] - a[0][1] * a[2][0])
            adj[0][2] = (a[0][1] * a[1][2] - a[0][2] * a[1][1])
            adj[1][2] = -(a[0][0] * a[1][2] - a[0][2] * a[1][0])
            adj[2][2] = (a[0][0] * a[1][1] - a[0][1] * a[1][0])

            Array(3) { i -> DoubleArray(3) { j -> adj[i][j] / det } }
        }
        displayResult(inv)
    }

    private fun calculateTranspose() {
        val a = getMatrix(gridA, true)
        if (a == null) {
            showError(getString(R.string.error_incomplete_matrix))
            return
        }
        val res = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                res[j][i] = a[i][j]
            }
        }
        displayResult(res)
    }

    private fun clearMatrices() {
        for (i in 0 until gridA.childCount) {
            (gridA.getChildAt(i) as EditText).text.clear()
            (gridB.getChildAt(i) as EditText).text.clear()
        }
        resultLabel.visibility = View.GONE
        resultDisplay.visibility = View.GONE
    }

    private fun showError(message: String) {
        resultLabel.visibility = View.VISIBLE
        resultDisplay.visibility = View.VISIBLE
        resultDisplay.text = message
    }

    private fun displayResult(matrix: Array<DoubleArray>) {
        val sb = StringBuilder()
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                sb.append(String.format(Locale.US, "%.2f", matrix[i][j])).append("  ")
            }
            sb.append("\n")
        }
        val resultText = sb.toString().trim()
        resultLabel.visibility = View.VISIBLE
        resultDisplay.visibility = View.VISIBLE
        resultDisplay.text = resultText

        // Save to History
        HistoryHelper.saveEntry(this, getString(R.string.matrix_mode), resultText.replace("\n", " | "))
    }
}
