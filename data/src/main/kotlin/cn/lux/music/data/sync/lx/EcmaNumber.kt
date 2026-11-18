package cn.lux.music.data.sync.lx

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/** Shortest decimal which rounds back to the same IEEE-754 number, as used by JSON.stringify. */
internal object EcmaNumber {
    private val syntax = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

    fun isValidToken(token: String): Boolean = syntax.matches(token)

    fun stringify(token: String): String {
        if (!isValidToken(token)) throw LxProtocolException("Invalid JSON number")
        val number = token.toDoubleOrNull() ?: throw LxProtocolException("Invalid JSON number")
        return when {
            !number.isFinite() -> "null"
            number == 0.0 -> "0"
            abs(number) <= 9007199254740991.0 && number == number.toLong().toDouble() -> number.toLong().toString()
            else -> formatNumber(number)
        }
    }

    private fun formatNumber(number: Double): String {
        val exact = BigDecimal(abs(number))
        val shortest = shortest(exact, abs(number))
        val digits = shortest.unscaledValue().toString()
        val exponent = digits.length - shortest.scale() - 1
        val unsigned =
            if (exponent in -6..20) {
                shortest.toPlainString()
            } else {
                val fraction = if (digits.length > 1) "." + digits.substring(1) else ""
                val sign = if (exponent >= 0) "+" else ""
                "${digits[0]}${fraction}e$sign$exponent"
            }
        return if (number < 0) "-$unsigned" else unsigned
    }

    private fun shortest(
        exact: BigDecimal,
        number: Double,
    ): BigDecimal {
        for (precision in 1..17) {
            val rounded = exact.round(MathContext(precision, RoundingMode.HALF_EVEN))
            val step = BigDecimal.ONE.scaleByPowerOfTen(exact.precision() - exact.scale() - precision)
            val valid =
                listOf(rounded, rounded - step, rounded + step)
                    .map(BigDecimal::stripTrailingZeros)
                    .filter { it.signum() > 0 && it.precision() <= precision && it.toDouble() == number }
            if (valid.isNotEmpty()) {
                return valid.minWith(
                    compareBy<BigDecimal> { (it - exact).abs() }
                        .thenBy { it.unscaledValue().testBit(0) },
                )
            }
        }
        throw LxProtocolException("Cannot represent JSON number")
    }
}
