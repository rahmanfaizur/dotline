package dev.rahmanfaizur.dotline.domain

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow

/**
 * Works out simple sums typed into search ("12*8", "(3+4)/2", "15% of 80" is not supported,
 * "2^10" is). Only digits, + - * / x × ÷ ^ % and brackets; anything else isn't a sum.
 */
object Calculator {
    /** The answer, or null if [text] isn't a sum (a lone number isn't one either). */
    fun evaluate(text: String): Double? {
        val s = text.replace(" ", "").replace('×', '*').replace('x', '*').replace('X', '*').replace('÷', '/').replace(',', '.')
        if (s.isEmpty() || s.any { it !in "0123456789.+-*/^%()" }) return null
        if (s.none { it in "+-*/^%" } || s.none { it.isDigit() }) return null
        return runCatching {
            val p = Parser(s)
            val v = p.expr()
            if (p.pos != s.length || v.isNaN() || v.isInfinite()) null else v
        }.getOrNull()
    }

    /** "96", "0.333333333", "1.5E12": short and without trailing zeros. */
    fun format(v: Double): String {
        val bd = BigDecimal(v).round(MathContext(10)).stripTrailingZeros()
        return if (bd.abs() >= BigDecimal("1e12") || (bd.signum() != 0 && bd.abs() < BigDecimal("1e-6"))) bd.toString() else bd.toPlainString()
    }

    private class Parser(val s: String) {
        var pos = 0
        private fun peek() = s.getOrNull(pos)
        fun expr(): Double {
            var v = term()
            while (true) v = when (peek()) {
                '+' -> { pos++; v + term() }
                '-' -> { pos++; v - term() }
                else -> return v
            }
        }
        private fun term(): Double {
            var v = power()
            while (true) v = when (peek()) {
                '*' -> { pos++; v * power() }
                '/' -> { pos++; v / power() }
                '%' -> { pos++; v % power() }
                else -> return v
            }
        }
        private fun power(): Double {
            val base = unary()
            return if (peek() == '^') { pos++; base.pow(power()) } else base
        }
        private fun unary(): Double = when (peek()) {
            '-' -> { pos++; -unary() }
            '+' -> { pos++; unary() }
            else -> atom()
        }
        private fun atom(): Double {
            if (peek() == '(') {
                pos++
                val v = expr()
                require(peek() == ')')
                pos++
                return v
            }
            val start = pos
            while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
            require(pos > start)
            return s.substring(start, pos).toDouble()
        }
    }
}
