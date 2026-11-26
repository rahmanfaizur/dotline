package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {
    private fun calc(s: String) = Calculator.evaluate(s)?.let(Calculator::format)

    @Test fun sums() {
        assertEquals("96", calc("12*8"))
        assertEquals("96", calc("12 x 8"))
        assertEquals("3.5", calc("(3+4)/2"))
        assertEquals("1024", calc("2^10"))
        assertEquals("-2", calc("-5+3"))
        assertEquals("0.3333333333", calc("1/3"))
        assertEquals("4", calc("10÷2.5"))
    }

    @Test fun notSums() {
        assertNull(calc("42"))       // a lone number
        assertNull(calc("spotify"))
        assertNull(calc("x"))        // a letter that only looks like times
        assertNull(calc("2+"))
        assertNull(calc("1/0"))
        assertNull(calc("(2+3"))
    }
}
