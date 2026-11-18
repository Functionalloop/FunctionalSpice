package com.functional.spice.data.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.dantsu.escposprinter.EscPosPrinterSize
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.functional.spice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Draws the "[logo] Functional" strip printed near the end of every customer bill:
 * 384 x 72 px (full 48mm width), pure black and white.
 *
 *   val hex = footerStrip.hex(printer)
 *   printer.printFormattedText(ReceiptFormatter.customerBill(bill, shop, hex))
 *
 * The hex string is built once and cached. If image printing fails, the printer manager
 * prints the bill again with the text "Functional" instead.
 */
@Singleton
class FooterStripRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WIDTH = 384   // 48mm at 203 dpi
        // Mark 56 dots tall: below ~50 the thermal head fills the logo's thin gaps (tested on paper)
        const val HEIGHT = 72
        private const val MARK_H = 56
        private const val GAP = 17
        private const val WORD = "Functional"
    }

    private var cacheHex: String? = null

    @Synchronized
    fun hex(printer: EscPosPrinterSize): String =
        cacheHex ?: PrinterTextParserImg.bitmapToHexadecimalString(printer, render()).also { cacheHex = it }

    fun render(): Bitmap {
        val strip = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(strip).apply { drawColor(Color.WHITE) }

        val mark = ContextCompat.getDrawable(context, R.drawable.functional_logo)!!.mutate()
        val markW = MARK_H * mark.intrinsicWidth / mark.intrinsicHeight

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 42f
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_extrabold) }
                .getOrNull() ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val startX = ((WIDTH - (markW + GAP + paint.measureText(WORD))) / 2f).toInt()
        val markTop = (HEIGHT - MARK_H) / 2
        mark.setBounds(startX, markTop, startX + markW, markTop + MARK_H)
        mark.setTint(Color.BLACK)
        mark.draw(c)

        // Vertically centre the word on the mark
        val baseline = HEIGHT / 2f - (paint.ascent() + paint.descent()) / 2f
        c.drawText(WORD, (startX + markW + GAP).toFloat(), baseline, paint)

        return strip.toThermalMonochrome()
    }
}
