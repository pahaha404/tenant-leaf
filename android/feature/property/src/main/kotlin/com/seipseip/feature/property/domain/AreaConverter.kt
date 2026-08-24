package com.seipseip.feature.property.domain

import kotlin.math.round

object AreaConverter {
    const val SQUARE_METERS_PER_PYEONG: Double = 3.305785

    fun squareMetersToPyeong(squareMeters: Double): Double = squareMeters / SQUARE_METERS_PER_PYEONG

    fun pyeongToSquareMeters(pyeong: Double): Double = pyeong * SQUARE_METERS_PER_PYEONG

    fun roundForDisplay(value: Double, decimalPlaces: Int = 2): Double {
        val scale = 10.0.pow(decimalPlaces)
        return round(value * scale) / scale
    }
}

private fun Double.pow(exponent: Int): Double {
    var result = 1.0
    repeat(exponent.coerceAtLeast(0)) { result *= this }
    return result
}

