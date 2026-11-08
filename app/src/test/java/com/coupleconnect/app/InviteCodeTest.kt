package com.coupleconnect.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InviteCodeTest {
    @Test fun sixDigitCodeRangeHasSixDigits() {
        val code = (100000..999999).random().toString()
        assertEquals(6, code.length)
    }

    @Test fun mobileInputCanBeLimitedToFifteenDigits() {
        val input = "123456789012345678"
        assertEquals(15, input.filter(Char::isDigit).take(15).length)
    }
}
