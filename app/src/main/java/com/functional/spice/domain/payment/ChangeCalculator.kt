package com.functional.spice.domain.payment

/** All amounts are whole rupees. */
object ChangeCalculator {
    // List, not IntArray: IntArray has no mapNotNull (the pack's version didn't compile)
    private val DENOMS = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)

    /** Positive = give back, negative = still to collect, 0 = exact. */
    fun change(total: Int, received: Int): Int = received - total

    /** Quick-amount chips. 230 -> [230, 250, 300, 500]; 300 -> [300, 350, 400, 500]. */
    fun suggestions(total: Int): List<Int> {
        val out = linkedSetOf(total)
        for (d in intArrayOf(50, 100, 500, 1000, 2000)) {
            out += (total / d + 1) * d          // next multiple strictly above total
            if (out.size >= 4) break
        }
        return out.sorted().take(4)
    }

    /** Fewest notes and coins. 70 -> [(50, 1), (20, 1)]. */
    fun breakdown(change: Int): List<Pair<Int, Int>> {
        var left = change
        return DENOMS.mapNotNull { d ->
            (left / d).takeIf { it > 0 }?.let { n -> left -= n * d; d to n }
        }
    }
}
