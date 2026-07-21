package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

// --- Calculator state machine ---

private enum class Op { ADD, SUBTRACT, MULTIPLY, DIVIDE }

private class CalculatorState {
    var display by mutableStateOf("0")
    private var storedValue: Double? = null
    private var pendingOp: Op? = null
    private var justEvaluated = false
    private var newInputPending = true

    private val fmt = DecimalFormat("#,##0.##########")

    fun onDigit(d: String) {
        if (newInputPending || justEvaluated) {
            display = if (d == "0") "0" else d
            newInputPending = false
            justEvaluated = false
        } else {
            display = if (display == "0") d else display + d
        }
    }

    fun onDecimal() {
        if (newInputPending || justEvaluated) {
            display = "0."
            newInputPending = false
            justEvaluated = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun onClear() {
        display = "0"
        storedValue = null
        pendingOp = null
        justEvaluated = false
        newInputPending = true
    }

    fun onToggleSign() {
        val v = display.toDoubleOrNull() ?: return
        display = formatResult(-v)
    }

    fun onPercent() {
        val v = display.toDoubleOrNull() ?: return
        display = formatResult(v / 100.0)
    }

    fun onOperator(op: Op) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (storedValue != null && pendingOp != null && !newInputPending) {
            storedValue = compute(storedValue!!, current, pendingOp!!)
            display = formatResult(storedValue!!)
        } else {
            storedValue = current
        }
        pendingOp = op
        newInputPending = true
        justEvaluated = false
    }

    fun onEquals() {
        val current = display.toDoubleOrNull() ?: 0.0
        val op = pendingOp
        val stored = storedValue
        if (op != null && stored != null) {
            val result = compute(stored, current, op)
            display = formatResult(result)
        }
        pendingOp = null
        storedValue = null
        newInputPending = true
        justEvaluated = true
    }

    private fun compute(a: Double, b: Double, op: Op): Double = when (op) {
        Op.ADD -> a + b
        Op.SUBTRACT -> a - b
        Op.MULTIPLY -> a * b
        Op.DIVIDE -> if (b == 0.0) Double.NaN else a / b
    }

    private fun formatResult(v: Double): String {
        if (v.isNaN()) return "Error"
        if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e15) {
            return v.toLong().toString()
        }
        return fmt.format(v)
    }
}

@Composable
private fun CalculatorScreen() {
    val state = remember { CalculatorState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = state.display,
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val buttonRows = listOf(
            listOf("C", "+/-", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        buttonRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    val weight = if (label == "0") 2f else 1f
                    CalcButton(
                        label = label,
                        weight = weight,
                        modifier = Modifier.weight(weight)
                    ) {
                        handleButton(label, state)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun handleButton(label: String, state: CalculatorState) {
    when (label) {
        in "0".."9" -> state.onDigit(label)
        "." -> state.onDecimal()
        "C" -> state.onClear()
        "+/-" -> state.onToggleSign()
        "%" -> state.onPercent()
        "÷" -> state.onOperator(Op.DIVIDE)
        "×" -> state.onOperator(Op.MULTIPLY)
        "−" -> state.onOperator(Op.SUBTRACT)
        "+" -> state.onOperator(Op.ADD)
        "=" -> state.onEquals()
    }
}

@Composable
private fun CalcButton(
    label: String,
    weight: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOperator = label in listOf("÷", "×", "−", "+", "=")
    val isFunction = label in listOf("C", "+/-", "%")

    val bgColor = when {
        isOperator -> Color(0xFFFF9F0A)
        isFunction -> Color(0xFFA5A5A5)
        else -> Color(0xFF333333)
    }
    val textColor = if (isFunction) Color.Black else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
