package com.mrayush.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {
    @Test
    fun decimalAdditionIsExact() {
        assertSuccess(Operation.PLUS, "0.1", "0.2", "0.3")
    }

    @Test
    fun largeMultiplicationPreservesEveryDigit() {
        assertSuccess(
            Operation.MUL,
            "999999999999",
            "999999999999",
            "999999999998000000000001"
        )
    }

    @Test
    fun divisionUsesDecimal128Precision() {
        assertSuccess(
            Operation.DVD,
            "1",
            "3",
            "0.3333333333333333333333333333333333"
        )
    }

    @Test
    fun factorialIsExactAndIterative() {
        assertSuccess(
            Operation.FACT,
            "50",
            "",
            "30414093201713378043612608166064768844377641568960512000000000000"
        )
    }

    @Test
    fun combinationIsExact() {
        assertSuccess(Operation.nCr, "100", "3", "161700")
    }

    @Test
    fun percentAndModuloKeepCalculatorSemantics() {
        assertSuccess(Operation.PERCENT, "25", "200", "50")
        assertSuccess(Operation.mod, "-7", "4", "-3")
    }

    @Test
    fun integerPowerIsExact() {
        assertSuccess(Operation.POW, "12", "10", "61917364224")
    }

    @Test
    fun unaryOperationUsesEnteredSecondOperand() {
        val outcome = CalculatorEngine.evaluate(Operation.sqrt, "", "81")
        assertEquals("9", CalculatorEngine.format((outcome as CalculationOutcome.Success).value))
    }

    @Test
    fun invalidDomainsReturnTypedErrors() {
        assertError(Operation.DVD, "5", "0", CalculationError.DIVISION_BY_ZERO)
        assertError(Operation.mod, "5", "0", CalculationError.DIVISION_BY_ZERO)
        assertError(Operation.sqrt, "-1", "", CalculationError.DOMAIN)
        assertError(Operation.log, "0", "", CalculationError.DOMAIN)
        assertError(Operation.FACT, "2.5", "", CalculationError.INTEGER_REQUIRED)
        assertError(Operation.nCr, "4", "5", CalculationError.DOMAIN)
        assertError(Operation.inv, "0", "", CalculationError.DIVISION_BY_ZERO)
    }

    @Test
    fun tangentAtNinetyDegreesReturnsDomainError() {
        assertError(Operation.tan, "90", "", CalculationError.DOMAIN)
    }

    @Test
    fun trigonometryDefaultsToDegrees() {
        val outcome = CalculatorEngine.evaluate(Operation.sin, "", "90")
        assertEquals("1", CalculatorEngine.format((outcome as CalculationOutcome.Success).value))
    }

    @Test
    fun trigonometryRespectsRadianMode() {
        val outcome = CalculatorEngine.evaluate(
            Operation.sin,
            "",
            "1.5707963267948966",
            AngleUnit.RADIANS
        )
        assertEquals("1", CalculatorEngine.format((outcome as CalculationOutcome.Success).value))
    }

    @Test
    fun tangentAtNinetyRadiansIsFinite() {
        val outcome = CalculatorEngine.evaluate(Operation.tan, "90", "", AngleUnit.RADIANS)
        assertTrue("Expected success but was $outcome", outcome is CalculationOutcome.Success)
    }

    @Test
    fun unsafePowerReturnsComputeLimitError() {
        assertError(Operation.POW, "10", "10001", CalculationError.RESULT_TOO_LARGE)
        assertError(Operation.FACT, "2001", "", CalculationError.RESULT_TOO_LARGE)
        assertError(Operation.e, "1000", "", CalculationError.RESULT_TOO_LARGE)
    }

    @Test
    fun formatterNeverUsesScientificNotation() {
        assertEquals("1000000000000", CalculatorEngine.format(BigDecimal("1E+12")))
        assertEquals("0", CalculatorEngine.format(BigDecimal("-0.000")))
    }

    private fun assertSuccess(
        operation: Operation,
        first: String,
        second: String,
        expected: String
    ) {
        val outcome = CalculatorEngine.evaluate(operation, first, second)
        assertTrue("Expected success but was $outcome", outcome is CalculationOutcome.Success)
        assertEquals(
            expected,
            CalculatorEngine.format((outcome as CalculationOutcome.Success).value)
        )
    }

    private fun assertError(
        operation: Operation,
        first: String,
        second: String,
        expected: CalculationError
    ) {
        assertEquals(
            CalculationOutcome.Error(expected),
            CalculatorEngine.evaluate(operation, first, second)
        )
    }
}
