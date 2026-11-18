package com.functional.spice.domain.model

/**
 * Editable shop details (stored in DataStore). Used on screen, on every slip and for UPI.
 */
data class ShopConfig(
    /** Shown on screen, e.g. the Counter title. */
    val shopName: String = "Local Spice",
    /** Printed header in the big font: max 16 characters fit on 58mm. */
    val printName: String = "LOCAL SPICE",
    /** Printed letter-spaced under the name ("T E A   S T A L L"). Blank prints nothing. */
    val subtitle: String = "TEA STALL",
    /** Used in "Built for Deu" at the foot of every bill. */
    val displayName: String = "Deu",
    val address: String = "Canacona, Goa",
    /** Was the footer-card QR ref. The v2.1 bill has no QR; kept so saved settings still load. */
    val slug: String = "deus-tea-stall",
    /** Functional's number, printed at the foot of every bill. */
    val supportPhone: String = "93220 53720",
    val upiId: String = "deusteastall@upi",
    /** Bills print only when the total is at least this many rupees; 0 prints every bill. */
    val minPrintAmount: Int = 0
)
