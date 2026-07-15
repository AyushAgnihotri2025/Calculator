package com.mrayush.calculator

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

sealed interface CalculationOutcome {
    data class Success(val value: BigDecimal) : CalculationOutcome
    data class Error(val type: CalculationError) : CalculationOutcome
}

enum class CalculationError {
    INVALID_NUMBER,
    DIVISION_BY_ZERO,
    DOMAIN,
    INTEGER_REQUIRED,
    RESULT_TOO_LARGE
}

/** Unit used to interpret the input of trigonometric functions. */
enum class AngleUnit {
    DEGREES,
    RADIANS
}

/**
 * Pure calculator logic. Decimal arithmetic remains exact where the operation permits it.
 * Non-terminating and transcendental results use DECIMAL128 or the precision available from
 * java.lang.Math respectively.
 */
object CalculatorEngine {
    private val mathContext = MathContext.DECIMAL128
    private val oneHundred = BigDecimal("100")
    private const val maxIntegerOperationInput = 2_000
    private const val maxPower = 10_000
    private const val maxResultDigits = 100_000

    fun evaluate(
        operation: Operation,
        firstOperand: String,
        secondOperand: String,
        angleUnit: AngleUnit = AngleUnit.DEGREES
    ): CalculationOutcome {
        return try {
            val first = firstOperand.toBigDecimalOrNull()
            val second = secondOperand.toBigDecimalOrNull()
            when (operation) {
                Operation.EMPTY -> first.successOrInvalid()
                Operation.PLUS -> binary(first, second) { left, right -> left.add(right) }
                Operation.MINUS -> binary(first, second) { left, right -> left.subtract(right) }
                Operation.MUL -> binary(first, second) { left, right -> left.multiply(right) }
                Operation.DVD -> {
                    if (second?.signum() == 0) error(CalculationError.DIVISION_BY_ZERO)
                    else binary(first, second) { left, right -> left.divide(right, mathContext) }
                }
                Operation.PERCENT ->
                    binary(first, second) { left, right ->
                        left.divide(oneHundred).multiply(right)
                    }
                Operation.mod -> {
                    if (second?.signum() == 0) error(CalculationError.DIVISION_BY_ZERO)
                    else binary(first, second) { left, right -> left.remainder(right) }
                }
                Operation.POW -> power(first, second)
                Operation.nCr -> combination(first, second)
                Operation.FACT -> factorial(unaryOperand(first, second))
                Operation.inv -> {
                    val operand = unaryOperand(first, second)
                    when {
                        operand == null -> error(CalculationError.INVALID_NUMBER)
                        operand.signum() == 0 -> error(CalculationError.DIVISION_BY_ZERO)
                        else -> CalculationOutcome.Success(BigDecimal.ONE.divide(operand, mathContext))
                    }
                }
                Operation.sqrt -> approximateUnary(first, second) { value ->
                    if (value < 0.0) null else sqrt(value)
                }
                Operation.log -> approximateUnary(first, second) { value ->
                    if (value <= 0.0) null else log10(value)
                }
                Operation.ln -> approximateUnary(first, second) { value ->
                    if (value <= 0.0) null else ln(value)
                }
                Operation.sin -> approximateUnary(first, second) {
                    sin(it.toAngleRadians(angleUnit))
                }
                Operation.cos -> approximateUnary(first, second) {
                    cos(it.toAngleRadians(angleUnit))
                }
                Operation.tan -> approximateUnary(first, second) {
                    val radians = it.toAngleRadians(angleUnit)
                    if (abs(cos(radians)) < 1e-12) null else tan(radians)
                }
                Operation.e -> approximateUnary(first, second) { exp(it) }
            }
        } catch (_: ArithmeticException) {
            error(CalculationError.RESULT_TOO_LARGE)
        }
    }

    fun format(value: BigDecimal): String {
        val normalized = if (value.signum() == 0) BigDecimal.ZERO else value.stripTrailingZeros()
        return normalized.toPlainString()
    }

    private fun BigDecimal?.successOrInvalid(): CalculationOutcome =
        if (this == null) error(CalculationError.INVALID_NUMBER)
        else CalculationOutcome.Success(this)

    private inline fun binary(
        first: BigDecimal?,
        second: BigDecimal?,
        calculation: (BigDecimal, BigDecimal) -> BigDecimal
    ): CalculationOutcome {
        if (first == null || second == null) return error(CalculationError.INVALID_NUMBER)
        val result = calculation(first, second)
        return if (estimatedPlainLength(result) > maxResultDigits) {
            error(CalculationError.RESULT_TOO_LARGE)
        } else {
            CalculationOutcome.Success(result)
        }
    }

    private fun power(base: BigDecimal?, exponent: BigDecimal?): CalculationOutcome {
        if (base == null || exponent == null) return error(CalculationError.INVALID_NUMBER)
        val integerExponent = exponent.toBigIntegerExactOrNull()
        if (integerExponent != null) {
            if (integerExponent.abs() > BigInteger.valueOf(maxPower.toLong())) {
                return error(CalculationError.RESULT_TOO_LARGE)
            }
            val exponentInt = integerExponent.toInt()
            if (base.signum() == 0 && exponentInt < 0) {
                return error(CalculationError.DIVISION_BY_ZERO)
            }
            val estimatedDigits =
                (base.precision().toLong() * abs(exponentInt.toLong())).coerceAtLeast(1)
            val estimatedScale = abs(base.scale().toLong() * exponentInt.toLong())
            if (estimatedDigits > maxResultDigits || estimatedScale > maxResultDigits) {
                return error(CalculationError.RESULT_TOO_LARGE)
            }
            val result = if (exponentInt >= 0) {
                base.pow(exponentInt)
            } else {
                BigDecimal.ONE.divide(base.pow(-exponentInt), mathContext)
            }
            return if (estimatedPlainLength(result) > maxResultDigits) {
                error(CalculationError.RESULT_TOO_LARGE)
            } else {
                CalculationOutcome.Success(result)
            }
        }

        val result = base.toDouble().pow(exponent.toDouble())
        return finiteApproximation(result)
    }

    private fun factorial(operand: BigDecimal?): CalculationOutcome {
        val integer = operand?.toBigIntegerExactOrNull()
            ?: return error(
                if (operand == null) CalculationError.INVALID_NUMBER
                else CalculationError.INTEGER_REQUIRED
            )
        if (integer.signum() < 0) return error(CalculationError.DOMAIN)
        if (integer > BigInteger.valueOf(maxIntegerOperationInput.toLong())) {
            return error(CalculationError.RESULT_TOO_LARGE)
        }

        var result = BigInteger.ONE
        for (factor in 2..integer.toInt()) {
            result = result.multiply(BigInteger.valueOf(factor.toLong()))
        }
        return CalculationOutcome.Success(result.toBigDecimal())
    }

    private fun combination(nValue: BigDecimal?, rValue: BigDecimal?): CalculationOutcome {
        val n = nValue?.toBigIntegerExactOrNull()
        val r = rValue?.toBigIntegerExactOrNull()
        if (n == null || r == null) {
            return error(
                if (nValue == null || rValue == null) CalculationError.INVALID_NUMBER
                else CalculationError.INTEGER_REQUIRED
            )
        }
        if (n.signum() < 0 || r.signum() < 0 || r > n) {
            return error(CalculationError.DOMAIN)
        }
        if (n > BigInteger.valueOf(maxIntegerOperationInput.toLong())) {
            return error(CalculationError.RESULT_TOO_LARGE)
        }

        val smallerR = r.min(n.subtract(r)).toInt()
        var result = BigInteger.ONE
        for (index in 1..smallerR) {
            result = result.multiply(n.subtract(BigInteger.valueOf((index - 1).toLong())))
                .divide(BigInteger.valueOf(index.toLong()))
        }
        return CalculationOutcome.Success(result.toBigDecimal())
    }

    private inline fun approximateUnary(
        first: BigDecimal?,
        second: BigDecimal?,
        calculation: (Double) -> Double?
    ): CalculationOutcome {
        val operand = unaryOperand(first, second) ?: return error(CalculationError.INVALID_NUMBER)
        val input = operand.toDouble()
        if (!input.isFinite()) return error(CalculationError.RESULT_TOO_LARGE)
        val result = calculation(input) ?: return error(CalculationError.DOMAIN)
        return finiteApproximation(result)
    }

    private fun finiteApproximation(value: Double): CalculationOutcome = when {
        value.isNaN() -> error(CalculationError.DOMAIN)
        value.isInfinite() -> error(CalculationError.RESULT_TOO_LARGE)
        else -> CalculationOutcome.Success(
            BigDecimal.valueOf(value).round(MathContext(16, RoundingMode.HALF_EVEN))
        )
    }

    private fun unaryOperand(first: BigDecimal?, second: BigDecimal?): BigDecimal? =
        second ?: first

    private fun Double.toAngleRadians(angleUnit: AngleUnit): Double = when (angleUnit) {
        AngleUnit.DEGREES -> Math.toRadians(this)
        AngleUnit.RADIANS -> this
    }

    private fun BigDecimal.toBigIntegerExactOrNull(): BigInteger? =
        try {
            toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }

    private fun estimatedPlainLength(value: BigDecimal): Int = when {
        value.scale() <= 0 -> value.precision() - value.scale()
        value.scale() >= value.precision() -> value.scale() + 2
        else -> value.precision() + 1
    }

    private fun error(type: CalculationError) = CalculationOutcome.Error(type)
}
