package com.functional.spice.data.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinterSize
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.functional.spice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Draws the chai cup printed at the top of every customer bill: R.drawable.cup_logo centred
 * on a 384 x 72 px strip (full 48mm width), pure black and white.
 *
 * The hex string is built once and cached, like [FooterStripRenderer].
 */
@Singleton
class CupLogoRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WIDTH = 384   // 48mm at 203 dpi
        const val HEIGHT = 72
        private const val CUP_H = 64
    }

    private var cacheHex: String? = null

    @Synchronized
    fun hex(printer: EscPosPrinterSize): String =
        cacheHex ?: PrinterTextParserImg.bitmapToHexadecimalString(printer, render()).also { cacheHex = it }

    fun render(): Bitmap {
        val strip = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(strip).apply { drawColor(Color.WHITE) }

        val cup = ContextCompat.getDrawable(context, R.drawable.cup_logo)!!.mutate()
        val cupW = CUP_H * cup.intrinsicWidth / cup.intrinsicHeight
        val left = (WIDTH - cupW) / 2
        val top = (HEIGHT - CUP_H) / 2
        cup.setBounds(left, top, left + cupW, top + CUP_H)
        cup.setTint(Color.BLACK)
        cup.draw(c)

        return strip.toThermalMonochrome()
    }
}

/** Hard black/white threshold: no greys, so the thermal head prints crisp edges. */
internal fun Bitmap.toThermalMonochrome(): Bitmap {
    val px = IntArray(width * height)
    getPixels(px, 0, width, 0, 0, width, height)
    for (i in px.indices) {
        val p = px[i]
        val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
        px[i] = if (lum < 140) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(px, width, height, Bitmap.Config.ARGB_8888)
}
