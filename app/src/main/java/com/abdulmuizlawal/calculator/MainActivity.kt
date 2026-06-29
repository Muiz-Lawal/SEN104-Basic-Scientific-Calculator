package com.abdulmuizlawal.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.operator.Operator
import net.objecthunter.exp4j.function.Function
import java.text.DecimalFormat
import java.util.ArrayList
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var displayPrimary: TextView
    private lateinit var displaySecondary: TextView
    private var scientificGrid: View? = null
    private var modeToggleBtn: MaterialButton? = null
    
    private var lastNumeric: Boolean = false
    private var stateError: Boolean = false
    private var lastDot: Boolean = false
    private var isDegreeMode: Boolean = true
    private var lastResult: Double = 0.0
    private var isScientificMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        displayPrimary = findViewById(R.id.display_primary)
        displaySecondary = findViewById(R.id.display_secondary)
        scientificGrid = findViewById(R.id.scientific_grid)
        modeToggleBtn = findViewById(R.id.btn_mode_toggle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        displayPrimary.setOnLongClickListener {
            val text = displayPrimary.text.toString()
            if (text.isNotEmpty() && !stateError) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Calculator Result", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        initCalculator()
        setupModeToggle()
    }

    private fun initCalculator() {
        setNumericListeners()
        setOperatorListeners()
        setScientificListeners()
        setControlListeners()
        
        findViewById<MaterialButton>(R.id.btn_history).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showHistory()
        }
        findViewById<MaterialButton>(R.id.btn_matrix).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            startActivity(Intent(this, MatrixActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_statistics).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }

    private fun setupModeToggle() {
        modeToggleBtn?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            isScientificMode = !isScientificMode
            scientificGrid?.visibility = if (isScientificMode) View.VISIBLE else View.GONE
            modeToggleBtn?.text = if (isScientificMode) getString(R.string.basic) else getString(R.string.scientific)
        }
    }

    private fun setNumericListeners() {
        val numericIds = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        )
        val numericListener = { view: View ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val button = view as MaterialButton
            if (stateError) {
                displayPrimary.text = button.text
                stateError = false
            } else {
                displayPrimary.append(button.text)
            }
            lastNumeric = true
        }
        numericIds.forEach { id -> findViewById<MaterialButton>(id).setOnClickListener(numericListener) }
        findViewById<MaterialButton>(R.id.btn_dot).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (lastNumeric && !stateError && !lastDot) {
                displayPrimary.append(".")
                lastNumeric = false
                lastDot = true
            }
        }
    }

    private fun setOperatorListeners() {
        val operators = mapOf(
            R.id.btn_add to "+", R.id.btn_subtract to "-",
            R.id.btn_multiply to "x", R.id.btn_divide to "÷",
            R.id.btn_percent to "%", R.id.btn_power to "^"
        )
        operators.forEach { (id, op) ->
            findViewById<MaterialButton>(id).setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (lastNumeric && !stateError) {
                    displayPrimary.append(op)
                    lastNumeric = false
                    lastDot = false
                }
            }
        }
        findViewById<MaterialButton>(R.id.btn_brackets).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val text = displayPrimary.text.toString()
            if (text.isEmpty() || text.endsWith("(") || text.last() in "+-x÷^%") {
                displayPrimary.append("(")
                lastNumeric = false
            } else {
                displayPrimary.append(")")
                lastNumeric = true
            }
        }
    }

    private fun setScientificListeners() {
        val functions = mapOf(
            R.id.btn_sin to "sin(", R.id.btn_cos to "cos(", R.id.btn_tan to "tan(",
            R.id.btn_asin to "asin(", R.id.btn_acos to "acos(", R.id.btn_atan to "atan(",
            R.id.btn_sinh to "sinh(", R.id.btn_cosh to "cosh(", R.id.btn_tanh to "tanh(",
            R.id.btn_log to "log10(", R.id.btn_ln to "log(", R.id.btn_sqrt to "sqrt(",
            R.id.btn_sum to "sum(", R.id.btn_avg to "avg(", R.id.btn_min to "min(",
            R.id.btn_max to "max(", R.id.btn_abs to "abs(", R.id.btn_exp to "exp(",
            R.id.btn_log2 to "log2(", R.id.btn_ceil to "ceil(", R.id.btn_floor to "floor(",
            R.id.btn_rand to "rand(", R.id.btn_inv to "inv(",
            R.id.btn_med to "med(", R.id.btn_std to "std(", R.id.btn_var to "var("
        )
        functions.forEach { (id, func) ->
            findViewById<MaterialButton>(id).setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (!stateError) {
                    displayPrimary.append(func)
                    lastNumeric = false
                }
            }
        }
        findViewById<MaterialButton>(R.id.btn_comma).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (lastNumeric && !stateError) {
                displayPrimary.append(",")
                lastNumeric = false
            }
        }
        findViewById<MaterialButton>(R.id.btn_pi).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            appendConstant("π") 
        }
        findViewById<MaterialButton>(R.id.btn_e).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            appendConstant("e") 
        }
        findViewById<MaterialButton>(R.id.btn_deg_rad).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            isDegreeMode = !isDegreeMode
            (it as MaterialButton).text = if (isDegreeMode) getString(R.string.deg) else getString(R.string.rad)
        }
        findViewById<MaterialButton>(R.id.btn_nCr).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (!stateError) { displayPrimary.append("nCr("); lastNumeric = false } 
        }
        findViewById<MaterialButton>(R.id.btn_nPr).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (!stateError) { displayPrimary.append("nPr("); lastNumeric = false } 
        }
        findViewById<MaterialButton>(R.id.btn_factorial).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (lastNumeric && !stateError) { displayPrimary.append("!"); lastNumeric = true } 
        }
        findViewById<MaterialButton>(R.id.btn_ans).setOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (!stateError) { displayPrimary.append("ans"); lastNumeric = true } 
        }
    }

    private fun setControlListeners() {
        findViewById<MaterialButton>(R.id.btn_clear).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            displayPrimary.text = ""
            displaySecondary.text = ""
            lastNumeric = false
            stateError = false
            lastDot = false
        }
        findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val text = displayPrimary.text.toString()
            if (text.isNotEmpty()) {
                displayPrimary.text = text.dropLast(1)
                updateStateAfterDelete()
            }
        }
        findViewById<MaterialButton>(R.id.btn_delete).setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            displayPrimary.text = ""
            updateStateAfterDelete()
            true
        }
        findViewById<MaterialButton>(R.id.btn_equals).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onEqual() 
        }
    }

    private fun appendConstant(constant: String) {
        if (!stateError) {
            displayPrimary.append(constant)
            lastNumeric = true
        }
    }

    private fun updateStateAfterDelete() {
        val text = displayPrimary.text.toString()
        if (text.isEmpty()) {
            lastNumeric = false; lastDot = false; stateError = false
            return
        }
        val lastChar = text.last()
        lastNumeric = lastChar.isDigit() || lastChar == 'π' || lastChar == 'e' || lastChar == ')' || lastChar == '!' || text.endsWith("ans")
        val lastNumberPart = text.split(Regex("[+\\-x÷^%()]")).last()
        lastDot = lastNumberPart.contains(".")
    }

    private fun onEqual() {
        val txt = displayPrimary.text.toString()
        if (txt.isEmpty()) return
        var expressionStr = txt.replace("x", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("%", "/100")
            .replace("ans", "($lastResult)")
        
        // Handle nCr / nPr infix conversion (e.g., 5nCr2 -> nCr(5,2))
        expressionStr = expressionStr.replace(Regex("(\\d+)nCr(\\d+)"), "nCr($1,$2)")
            .replace(Regex("(\\d+)nPr(\\d+)"), "nPr($1,$2)")

        // Pre-process factorial (e.g., 5! -> fact(5))
        expressionStr = preProcessFactorials(expressionStr)

        expressionStr = expressionStr.replace(Regex("(\\d)([a-zA-Z(πe])"), "$1*$2")
            .replace(Regex("([)πe])(\\d)"), "$1*$2")
            .replace(Regex("([)πe])([a-zA-Z(])"), "$1*$2")

        val openBrackets = expressionStr.count { it == '(' }
        val closeBrackets = expressionStr.count { it == ')' }
        if (openBrackets > closeBrackets) expressionStr += ")".repeat(openBrackets - closeBrackets)

        expressionStr = preProcessExpression(expressionStr)
        
        try {
            val expression = ExpressionBuilder(expressionStr)
                .functions(
                    sumFunc, avgFunc, minFunc, maxFunc, nCrFunc, nPrFunc,
                    sinFunc, cosFunc, tanFunc, asinFunc, acosFunc, atanFunc,
                    sinhFunc, coshFunc, tanhFunc, absFunc, expFunc, log2Func, 
                    ceilFunc, floorFunc, randFunc, invFunc, factFunc
                ).build()
            val result = expression.evaluate()
            if (result.isNaN()) throw IllegalArgumentException("NaN")
            if (result.isInfinite()) throw ArithmeticException("Div0")
            
            val formattedResult = formatResult(result)
            HistoryHelper.saveEntry(this, txt, formattedResult)
            
            lastResult = result
            displaySecondary.text = txt
            displayPrimary.text = formattedResult
            lastNumeric = true
            lastDot = displayPrimary.text.contains(".")
            stateError = false
        } catch (ex: Exception) {
            displayPrimary.text = getString(R.string.error_invalid)
            stateError = true
            lastNumeric = false
        }
    }

    private fun formatResult(result: Double): String {
        val absVal = abs(result)
        return if (absVal != 0.0 && (absVal < 1e-7 || absVal >= 1e13)) {
            DecimalFormat("0.#######E0").format(result).replace("E", "e")
        } else if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            DecimalFormat("0.########").format(result)
        }
    }

    private fun preProcessFactorials(input: String): String {
        var result = input
        while (result.contains("!")) {
            val idx = result.indexOf("!")
            if (idx <= 0) break
            if (result[idx - 1] == ')') {
                val startIdx = findMatchingParenReverse(result, idx - 1)
                if (startIdx != -1) {
                    val content = result.substring(startIdx, idx)
                    result = result.replaceRange(startIdx..idx, "fact$content")
                } else break
            } else {
                var i = idx - 1
                while (i >= 0 && (result[i].isDigit() || result[i] == '.' || result[i].isLetter())) {
                    i--
                }
                val atom = result.substring(i + 1, idx)
                result = result.replaceRange(i + 1..idx, "fact($atom)")
            }
        }
        return result
    }

    private fun preProcessExpression(input: String): String {
        var result = input
        val funcs = listOf("sum", "avg", "min", "max", "med", "std", "var")
        while (true) {
            var bestStart = -1
            var bestFunc = ""
            for (f in funcs) {
                val idx = result.lastIndexOf("$f(")
                if (idx > bestStart) { bestStart = idx; bestFunc = f }
            }
            if (bestStart == -1) break
            val endIdx = findMatchingParen(result, bestStart + bestFunc.length)
            if (endIdx == -1) break
            val content = result.substring(bestStart + bestFunc.length + 1, endIdx)
            val args = splitArgs(content)
            val replacement = when (bestFunc) {
                "sum" -> "(" + args.joinToString("+") + ")"
                "avg" -> "((" + args.joinToString("+") + ")/" + args.size + ")"
                "min" -> nestBinary(args, "min_")
                "max" -> nestBinary(args, "max_")
                "med" -> {
                    // Median is tricky without full evaluation, so we use a custom function for fixed args
                    // or handle it if we can. For now, we'll use a 2-arg min/max nesting isn't possible.
                    // We'll fallback to a custom internal function that we'll define carefully.
                    "med_(${args.joinToString(",")})"
                }
                "var" -> {
                    val n = args.size
                    val sumSq = "(" + args.joinToString("+") { "($it)^2" } + ")"
                    val sum = "(" + args.joinToString("+") + ")"
                    "(($sumSq/$n) - ($sum/$n)^2)"
                }
                "std" -> {
                    val n = args.size
                    val sumSq = "(" + args.joinToString("+") { "($it)^2" } + ")"
                    val sum = "(" + args.joinToString("+") + ")"
                    "sqrt(($sumSq/$n) - ($sum/$n)^2)"
                }
                else -> ""
            }
            result = result.replaceRange(bestStart..endIdx, replacement)
        }
        return result.replace("min_", "min").replace("max_", "max")
    }

    private fun calcMedian(args: List<String>): String {
        if (args.isEmpty()) return "0"
        // ExpressionBuilder doesn't support list sorting inside, 
        // so we evaluate children first if they are simple numbers or we rely on the engine.
        // Actually, for a professional calculator, we should evaluate the args first.
        return "med_(${args.joinToString(",")})"
    }

    private fun calcStdDev(args: List<String>): String {
        if (args.isEmpty()) return "0"
        return "std_(${args.joinToString(",")})"
    }

    private fun calcVariance(args: List<String>): String {
        if (args.isEmpty()) return "0"
        return "var_(${args.joinToString(",")})"
    }

    private fun findMatchingParen(s: String, openIdx: Int): Int {
        var count = 0
        for (i in openIdx until s.length) {
            if (s[i] == '(') count++
            else if (s[i] == ')') {
                count--
                if (count == 0) return i
            }
        }
        return -1
    }

    private fun findMatchingParenReverse(s: String, closeIdx: Int): Int {
        var count = 0
        for (i in closeIdx downTo 0) {
            if (s[i] == ')') count++
            else if (s[i] == '(') {
                count--
                if (count == 0) return i
            }
        }
        return -1
    }

    private fun splitArgs(s: String): List<String> {
        val res = mutableListOf<String>()
        var current = StringBuilder(); var depth = 0
        for (char in s) {
            if (char == ',' && depth == 0) { res.add(current.toString().trim()); current = StringBuilder() }
            else { if (char == '(') depth++; if (char == ')') depth--; current.append(char) }
        }
        res.add(current.toString().trim())
        return res
    }

    private fun nestBinary(args: List<String>, func: String): String {
        if (args.isEmpty()) return "0"
        if (args.size == 1) return args[0]
        var res = args.last()
        for (i in args.size - 2 downTo 0) res = "$func(${args[i]}, $res)"
        return res
    }

    private fun showHistory() {
        val historyList = HistoryHelper.loadHistory(this)
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_history_bottom_sheet, null)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_history)
        val emptyTxt = view.findViewById<TextView>(R.id.txt_no_history)
        val clearBtn = view.findViewById<MaterialButton>(R.id.btn_clear_history)

        if (historyList.isEmpty()) emptyTxt.visibility = View.VISIBLE
        
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = HistoryAdapter(historyList) { item ->
            displayPrimary.text = item.second
            lastNumeric = true
            dialog.dismiss()
        }

        clearBtn.setOnClickListener {
            HistoryHelper.clearHistory(this)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private val sinFunc = object : Function("sin", 1) { override fun apply(vararg args: Double): Double { val angle = if (isDegreeMode) Math.toRadians(args[0]) else args[0]; val res = sin(angle); return if (abs(res) < 1e-15) 0.0 else res } }
    private val cosFunc = object : Function("cos", 1) { override fun apply(vararg args: Double): Double { val angle = if (isDegreeMode) Math.toRadians(args[0]) else args[0]; val res = cos(angle); return if (abs(res) < 1e-15) 0.0 else res } }
    private val tanFunc = object : Function("tan", 1) { override fun apply(vararg args: Double): Double { val angle = if (isDegreeMode) Math.toRadians(args[0]) else args[0]; if (abs(cos(angle)) < 1e-15) throw ArithmeticException(); val res = tan(angle); return if (abs(res) < 1e-15) 0.0 else res } }
    private val asinFunc = object : Function("asin", 1) { override fun apply(vararg args: Double): Double { val res = asin(args[0]); return if (isDegreeMode) Math.toDegrees(res) else res } }
    private val acosFunc = object : Function("acos", 1) { override fun apply(vararg args: Double): Double { val res = acos(args[0]); return if (isDegreeMode) Math.toDegrees(res) else res } }
    private val atanFunc = object : Function("atan", 1) { override fun apply(vararg args: Double): Double { val res = atan(args[0]); return if (isDegreeMode) Math.toDegrees(res) else res } }
    private val sinhFunc = object : Function("sinh", 1) { override fun apply(vararg args: Double): Double = sinh(args[0]) }
    private val coshFunc = object : Function("cosh", 1) { override fun apply(vararg args: Double): Double = cosh(args[0]) }
    private val tanhFunc = object : Function("tanh", 1) { override fun apply(vararg args: Double): Double = tanh(args[0]) }
    private val nCrFunc = object : Function("nCr", 2) { 
        override fun apply(vararg args: Double): Double {
            val n = args[0].toLong(); val r = args[1].toLong()
            if (r < 0 || r > n) return 0.0
            var res = 1.0; val k = if (r < n - r) r else n - r
            for (i in 1..k.toInt()) res = res * (n - i + 1) / i
            return res
        }
    }
    private val nPrFunc = object : Function("nPr", 2) {
        override fun apply(vararg args: Double): Double {
            val n = args[0].toLong(); val r = args[1].toLong()
            if (r < 0 || r > n) return 0.0
            var res = 1.0; for (i in 0 until r.toInt()) res *= (n - i)
            return res
        }
    }
    private val sumFunc = object : Function("sum", 2) { override fun apply(vararg args: Double): Double = args[0] + args[1] }
    private val avgFunc = object : Function("avg", 2) { override fun apply(vararg args: Double): Double = (args[0] + args[1]) / 2.0 }
    private val minFunc = object : Function("min", 2) { override fun apply(vararg args: Double): Double = min(args[0], args[1]) }
    private val maxFunc = object : Function("max", 2) { override fun apply(vararg args: Double): Double = max(args[0], args[1]) }
    private val absFunc = object : Function("abs", 1) { override fun apply(vararg args: Double): Double = abs(args[0]) }
    private val expFunc = object : Function("exp", 1) { override fun apply(vararg args: Double): Double = exp(args[0]) }
    private val log2Func = object : Function("log2", 1) { override fun apply(vararg args: Double): Double = log2(args[0]) }
    private val ceilFunc = object : Function("ceil", 1) { override fun apply(vararg args: Double): Double = ceil(args[0]) }
    private val floorFunc = object : Function("floor", 1) { override fun apply(vararg args: Double): Double = floor(args[0]) }
    private val randFunc = object : Function("rand", 0) { override fun apply(vararg args: Double): Double = Math.random() }
    private val invFunc = object : Function("inv", 1) { override fun apply(vararg args: Double): Double { if (args[0] == 0.0) throw ArithmeticException(); return 1.0 / args[0] } }

    private val medFunc = object : Function("med_", 1) {
        override fun apply(vararg args: Double): Double = args[0] // Fallback for single arg
    }

    private val factFunc = object : Function("fact", 1) {
        override fun apply(vararg args: Double): Double {
            val arg = args[0].toLong()
            if (arg < 0) return 0.0
            if (arg > 20) return Double.POSITIVE_INFINITY
            var res = 1.0
            for (i in 1..arg) res *= i
            return res
        }
    }

    inner class HistoryAdapter(private val history: List<Pair<String, String>>, private val onClick: (Pair<String, String>) -> Unit) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val exp: TextView = v.findViewById(R.id.history_expression)
            val res: TextView = v.findViewById(R.id.history_result)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = history[position]
            holder.exp.text = item.first
            holder.res.text = item.second
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount(): Int = history.size
    }
}
