package com.firestorm.llmath

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class LLCalcParser(private val variables: Map<String, Double> = emptyMap()) {

    private var pos = 0
    private var input = ""

    fun evaluate(expression: String): Double {
        input = expression.trim()
        pos = 0
        val result = parseExpr()
        if (pos < input.length) throw IllegalArgumentException("Unexpected character at pos $pos: '${input[pos]}'")
        return result
    }

    private fun skipWs() { while (pos < input.length && input[pos].isWhitespace()) pos++ }

    private fun parseExpr(): Double {
        var left = parseTerm()
        skipWs()
        while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
            val op = input[pos++]
            val right = parseTerm()
            left = if (op == '+') left + right else left - right
            skipWs()
        }
        return left
    }

    private fun parseTerm(): Double {
        var left = parseUnary()
        skipWs()
        while (pos < input.length && (input[pos] == '*' || input[pos] == '/' || input[pos] == '%')) {
            val op = input[pos++]
            val right = parseUnary()
            left = when (op) {
                '*' -> left * right
                '/' -> if (right == 0.0) throw ArithmeticException("Division by zero") else left / right
                else -> left % right
            }
            skipWs()
        }
        return left
    }

    private fun parseUnary(): Double {
        skipWs()
        if (pos < input.length && input[pos] == '-') { pos++; return -parsePower() }
        if (pos < input.length && input[pos] == '+') { pos++; return parsePower() }
        return parsePower()
    }

    private fun parsePower(): Double {
        val base = parsePrimary()
        skipWs()
        if (pos < input.length && input[pos] == '^') {
            pos++
            val exp = parseUnary()
            return Math.pow(base, exp)
        }
        return base
    }

    private fun parsePrimary(): Double {
        skipWs()
        if (pos >= input.length) throw IllegalArgumentException("Unexpected end of expression")
        if (input[pos] == '(') {
            pos++
            val v = parseExpr()
            skipWs()
            if (pos >= input.length || input[pos] != ')') throw IllegalArgumentException("Missing ')'")
            pos++
            return v
        }
        if (input[pos].isDigit() || (input[pos] == '.' && pos + 1 < input.length && input[pos + 1].isDigit())) {
            return parseNumber()
        }
        if (input[pos].isLetter() || input[pos] == '_') {
            return parseNamedValue()
        }
        throw IllegalArgumentException("Unexpected character '${input[pos]}' at pos $pos")
    }

    private fun parseNumber(): Double {
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
            pos++
            if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        return input.substring(start, pos).toDouble()
    }

    private fun parseNamedValue(): Double {
        val start = pos
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_')) pos++
        val name = input.substring(start, pos)
        skipWs()
        if (pos < input.length && input[pos] == '(') {
            pos++
            val args = mutableListOf<Double>()
            skipWs()
            if (pos < input.length && input[pos] != ')') {
                while (true) {
                    args += parseExpr()
                    skipWs()
                    if (pos < input.length && input[pos] == ',') {
                        pos++
                        skipWs()
                        continue
                    }
                    break
                }
            }
            if (pos >= input.length || input[pos] != ')') throw IllegalArgumentException("Missing ')'")
            pos++
            return applyFunction(name, args)
        }
        return when (name.lowercase()) {
            "pi" -> Math.PI
            "e"  -> Math.E
            else -> variables[name] ?: throw IllegalArgumentException("Unknown variable: $name")
        }
    }

    private fun applyFunction(name: String, args: List<Double>): Double {
        fun unary(block: (Double) -> Double): Double {
            require(args.size == 1) { "Function $name expects 1 argument" }
            return block(args[0])
        }

        fun binary(block: (Double, Double) -> Double): Double {
            require(args.size == 2) { "Function $name expects 2 arguments" }
            return block(args[0], args[1])
        }

        fun ternary(block: (Double, Double, Double) -> Double): Double {
            require(args.size == 3) { "Function $name expects 3 arguments" }
            return block(args[0], args[1], args[2])
        }

        return when (name.lowercase()) {
            "sqrt" -> unary { if (it < 0.0) Double.NaN else sqrt(it) }
            "abs" -> unary(::abs)
            "sin" -> unary(::sin)
            "cos" -> unary(::cos)
            "tan" -> unary(::tan)
            "asin" -> unary { if (it in -1.0..1.0) asin(it) else Double.NaN }
            "acos" -> unary { if (it in -1.0..1.0) acos(it) else Double.NaN }
            "atan" -> unary(::atan)
            "log" -> unary { if (it > 0.0) ln(it) else Double.NaN }
            "log10" -> unary { if (it > 0.0) log10(it) else Double.NaN }
            "exp" -> unary(::exp)
            "floor" -> unary(::floor)
            "ceil" -> unary(::ceil)
            "round" -> unary { round(it) }
            "min" -> binary(::minOf)
            "max" -> binary(::maxOf)
            "atan2" -> binary(::atan2)
            "clamp" -> ternary { value, min, max ->
                val lower = minOf(min, max)
                val upper = maxOf(min, max)
                value.coerceIn(lower, upper)
            }
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }
}
