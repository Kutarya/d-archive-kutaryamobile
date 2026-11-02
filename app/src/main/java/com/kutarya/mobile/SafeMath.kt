package com.kutarya.mobile

import kotlin.math.pow
import java.math.BigDecimal

/** Sınırlı ayrıştırıcı; kod, değişken ve fonksiyon çalıştırmaz. */
object SafeMath {
    fun calculate(source: String): String {
        require(source.length in 1..300) { "İşlem uzunluğu geçersiz." }
        val text = source.replace('×', '*').replace('÷', '/').replace(',', '.').filterNot { it.isWhitespace() }
        require(text.all { it.isDigit() || it in ".+-*/%^()" }) { "Yalnız sayı ve işlem işaretleri kullan." }
        var index = 0; var depth = 0
        fun finite(value: Double): Double { require(value.isFinite()) { "İşlem sonucu sonlu değil." }; return value }
        lateinit var expression: () -> Double
        lateinit var factor: () -> Double
        factor = {
            require(++depth <= 32) { "İşlem fazla iç içe." }
            val value = when {
                index < text.length && text[index] == '+' -> { index++; factor() }
                index < text.length && text[index] == '-' -> { index++; -factor() }
                index < text.length && text[index] == '(' -> {
                    index++; val v = expression(); require(index < text.length && text[index++] == ')') { "Parantez kapanmadı." }; v
                }
                else -> {
                    val start = index
                    while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
                    require(index > start) { "Sayı bekleniyor." }
                    text.substring(start, index).toDoubleOrNull() ?: throw IllegalArgumentException("Sayı biçimi geçersiz.")
                }
            }
            depth--; finite(value)
        }
        fun power(): Double {
            val a = factor()
            if (index < text.length && text[index] == '^') { index++; return finite(a.pow(factor())) }
            return a
        }
        fun term(): Double {
            var value = power()
            while (index < text.length && text[index] in "*/%") {
                val op = text[index++]; val right = power()
                require(op == '*' || right != 0.0) { "Sıfıra bölme yapılamaz." }
                value = finite(when(op) { '*' -> value * right; '/' -> value / right; else -> value % right })
            }
            return value
        }
        expression = {
            var value = term()
            while (index < text.length && text[index] in "+-") { val op = text[index++]; val right = term(); value = finite(if (op == '+') value + right else value - right) }
            value
        }
        val result = expression(); require(index == text.length) { "İşlem tamamlanamadı." }
        return BigDecimal.valueOf(result).stripTrailingZeros().toPlainString()
    }
}
