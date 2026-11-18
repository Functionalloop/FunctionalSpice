package com.functional.spice.domain.payment

import java.net.URLEncoder

object UpiLink {
    /**
     * upi://pay?pa={upiId}&pn=Deus%20Tea%20Stall&am={total}.00&cu=INR&tn=Bill%20{billNo}
     */
    fun build(upiId: String, payeeName: String, total: Int, billNo: Int): String {
        val pn = encode(payeeName.replace("'", ""))
        val tn = encode("Bill $billNo")
        return "upi://pay?pa=${upiId.trim()}&pn=$pn&am=$total.00&cu=INR&tn=$tn"
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
