package com.functional.spice.domain.model

import kotlin.math.abs

/** Rupee formatting for the screen: ₹ with Indian digit grouping (₹1,52,710). */
object Money {
    fun inr(rupees: Int): String = inr(rupees.toLong())

    fun inr(rupees: Long): String = (if (rupees < 0) "-₹" else "₹") + group(abs(rupees))

    /** Indian grouping without the symbol: 152710 -> "1,52,710". */
    fun group(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayDeque<String>()
        while (rest.length > 2) {
            parts.addFirst(rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.addFirst(rest)
        return parts.joinToString(",") + "," + last3
    }

    fun group(value: Int): String = group(value.toLong())
}
