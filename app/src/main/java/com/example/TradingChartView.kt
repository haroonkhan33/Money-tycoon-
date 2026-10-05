package com.example

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Locale

/**
 * High-performance, anti-aliased Canvas chart for live stock & crypto trading.
 * Renders smooth price paths, neon glowing gradient fills, and live price indicators.
 */
class TradingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val priceHistory = mutableListOf<Double>()
    private var isBullish = true

    // Paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141F33")
        strokeWidth = 1.5f
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 24f
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun updatePrices(prices: List<Double>, bullish: Boolean) {
        priceHistory.clear()
        priceHistory.addAll(prices)
        isBullish = bullish
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (priceHistory.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padTop = 20f
        val padBottom = 24f
        val padLeft = 16f
        val padRight = 16f
        val usableHeight = h - padTop - padBottom
        val usableWidth = w - padLeft - padRight

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padTop + (usableHeight / gridLines) * i
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint)
        }

        val minPrice = priceHistory.minOrNull() ?: 1.0
        val maxPrice = priceHistory.maxOrNull() ?: 100.0
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else (maxPrice - minPrice)

        val neonColor = if (isBullish) Color.parseColor("#00FF66") else Color.parseColor("#FF3366")
        val glowColor = if (isBullish) Color.parseColor("#4000FF66") else Color.parseColor("#40FF3366")

        linePaint.color = neonColor
        dotPaint.color = neonColor
        dotGlowPaint.color = glowColor

        linePath.reset()
        fillPath.reset()

        val stepX = usableWidth / (priceHistory.size - 1)
        var lastX = padLeft
        var lastY = padTop

        priceHistory.forEachIndexed { index, price ->
            val x = padLeft + index * stepX
            val normalized = (price - minPrice) / priceRange
            val y = padTop + usableHeight - (normalized.toFloat() * usableHeight)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h - padBottom)
                fillPath.lineTo(x, y)
            } else {
                // Smooth cubic bezier curve between points
                val prevX = padLeft + (index - 1) * stepX
                val prevNormalized = (priceHistory[index - 1] - minPrice) / priceRange
                val prevY = padTop + usableHeight - (prevNormalized.toFloat() * usableHeight)
                val midX = (prevX + x) / 2
                linePath.cubicTo(midX, prevY, midX, y, x, y)
                fillPath.cubicTo(midX, prevY, midX, y, x, y)
            }

            if (index == priceHistory.size - 1) {
                lastX = x
                lastY = y
            }
        }

        // Complete fill path
        fillPath.lineTo(lastX, h - padBottom)
        fillPath.close()

        // Gradient shader for fill
        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, h - padBottom,
            glowColor,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        // Draw fill & line
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Draw pulsing dot at the latest point
        canvas.drawCircle(lastX, lastY, 14f, dotGlowPaint)
        canvas.drawCircle(lastX, lastY, 6f, dotPaint)

        // Draw min and max labels
        canvas.drawText(String.format(Locale.US, "$%.2f", maxPrice), padLeft, padTop + 20f, textPaint)
        canvas.drawText(String.format(Locale.US, "$%.2f", minPrice), padLeft, h - padBottom - 8f, textPaint)
    }
}
