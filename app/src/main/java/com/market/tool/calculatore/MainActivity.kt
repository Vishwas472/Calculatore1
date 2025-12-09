package com.market.tool.calculatore

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Stack



class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var expression: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        // Numbers
        val numberButtons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btn00 to "00"
        )

        numberButtons.forEach { (id, text) ->
            findViewById<Button>(id).setOnClickListener { appendText(text) }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener { appendDot() }

        // Operators
        findViewById<Button>(R.id.btnAdd).setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { appendOperator("-") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { appendOperator("*") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { appendOperator("/") }

        // Functions
        findViewById<Button>(R.id.btnPercent).setOnClickListener { appendPercent() }
        findViewById<Button>(R.id.btnAC).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { backspace() }
        findViewById<Button>(R.id.btnEqual).setOnClickListener { calculateFinal() }
    }

    private fun appendText(txt: String) {
        if (expression.isEmpty() && txt == "00") return
        expression += txt
        updatePreview()
    }

    private fun appendDot() {
        val last = getLastNumber()
        if (!last.contains(".")) {
            expression += if (last.isEmpty()) "0." else "."
            updatePreview()
        }
    }

    private fun appendOperator(op: String) {
        if (expression.isEmpty()) {
            if (op == "-") expression = "-"
            return
        }
        if (expression.last().toString().matches(Regex("[+\\-*/]"))) {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        updatePreview()
    }

    private fun appendPercent() {
        if (expression.isNotEmpty() && expression.last().isDigit()) {
            expression += "%"
            updatePreview()
        }
    }

    private fun clearAll() {
        expression = ""
        tvExpression.text = ""
        tvResult.text = ""
    }

    private fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            updatePreview()
        }
    }

    private fun calculateFinal() {
        val result = evaluateExpression(expression)
        expression = if (result == null) "" else formatResult(result)
        tvExpression.text = expression
        tvResult.text = ""
    }

    private fun updatePreview() {
        tvExpression.text = expression
        if (expression.isEmpty()) {
            tvResult.text = ""
            return
        }
        val result = evaluateExpression(expression)
        tvResult.text = if (result == null) "" else formatResult(result)
    }

    private fun getLastNumber(): String {
        val sb = StringBuilder()
        for (c in expression.reversed()) {
            if (c.isDigit() || c == '.') sb.append(c) else break
        }
        return sb.reverse().toString()
    }

    private fun formatResult(value: Double): String {
        val str = value.toString()
        return if (str.endsWith(".0")) str.dropLast(2) else str
    }

    // ✅ Correct % behavior like Android calculator
    private fun evaluateExpression(exprRaw: String): Double? {
        if (exprRaw.isBlank()) return null
        try {
            var expr = exprRaw

            // Handle "10%545" → (10/100) * 545
            expr = handlePercentBetweenNumbers(expr)

            // Handle "90 + 10%" → 90 + (10% of 90)
            expr = handlePercentOfPrevious(expr)

            // Handle "50%" → 0.5
            expr = expr.replace(Regex("(\\d+(?:\\.\\d+)?)%")) {
                (it.groupValues[1].toDouble() / 100.0).toString()
            }

            return evaluateBasicMath(expr)

        } catch (e: Exception) {
            return null
        }
    }

    private fun handlePercentBetweenNumbers(expr: String): String {
        var s = expr
        val regex = Regex("(\\d+(?:\\.\\d+)?)%(\\d+(?:\\.\\d+)?)")
        while (true) {
            val m = regex.find(s) ?: break
            val a = m.groupValues[1].toDouble()
            val b = m.groupValues[2].toDouble()
            s = s.replaceRange(m.range, ((a / 100.0) * b).toString())
        }
        return s
    }

    private fun handlePercentOfPrevious(expr: String): String {
        var s = expr
        val regex = Regex("(\\d+(?:\\.\\d+)?)([+\\-])(\\d+(?:\\.\\d+)?)%")
        while (true) {
            val m = regex.find(s) ?: break
            val base = m.groupValues[1].toDouble()
            val op = m.groupValues[2]
            val percent = m.groupValues[3].toDouble()
            val value = (percent / 100.0) * base
            s = s.replaceRange(m.range, "$base$op$value")
        }
        return s
    }

    private fun evaluateBasicMath(expr: String): Double {
        val tokens = tokenize(expr)
        val rpn = toRPN(tokens)
        return evalRPN(rpn)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = ArrayList<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i]); i++
                    }
                    tokens.add(sb.toString())
                }
                c in "+-*/" -> {
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in "+-*/")) {
                        tokens.add("0")
                    }
                    tokens.add(c.toString())
                    i++
                }
                else -> throw Exception("Invalid char")
            }
        }
        return tokens
    }

    private fun precedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            else -> 0
        }
    }

    private fun toRPN(tokens: List<String>): List<String> {
        val output = ArrayList<String>()
        val ops = Stack<String>()
        for (t in tokens) {
            if (t.toDoubleOrNull() != null) {
                output.add(t)
            } else {
                while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(t)) {
                    output.add(ops.pop())
                }
                ops.push(t)
            }
        }
        while (ops.isNotEmpty()) output.add(ops.pop())
        return output
    }

    private fun evalRPN(rpn: List<String>): Double {
        val st = Stack<Double>()
        for (t in rpn) {
            val n = t.toDoubleOrNull()
            if (n != null) {
                st.push(n)
            } else {
                val b = st.pop()
                val a = st.pop()
                st.push(
                    when (t) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> a / b
                        else -> 0.0
                    }
                )
            }
        }
        return st.pop()
    }
}
