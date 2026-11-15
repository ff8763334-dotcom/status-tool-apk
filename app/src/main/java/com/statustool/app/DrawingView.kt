package com.statustool.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

data class TextElement(
    var text: String,
    var color: Int,
    var size: Float,
    var x: Float,
    var y: Float
)

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var baseBitmap: Bitmap? = null
    val elements = mutableListOf<TextElement>()
    var frameType: String = "none"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dragTarget: TextElement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    fun setImage(bmp: Bitmap) {
        baseBitmap = bmp
        elements.clear()
        invalidate()
    }

    fun addText(text: String, color: Int, size: Float) {
        if (baseBitmap == null || text.isBlank()) return
        elements.add(TextElement(text, color, size, width * 0.15f, height * 0.5f))
        invalidate()
    }

    fun undo() {
        if (elements.isNotEmpty()) elements.removeAt(elements.size - 1)
        invalidate()
    }

    fun hasImage(): Boolean = baseBitmap != null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = baseBitmap ?: return
        val dst = Rect(0, 0, width, height)
        canvas.drawBitmap(bmp, null, dst, paint)

        for (el in elements) {
            paint.color = el.color
            paint.textSize = el.size
            paint.style = Paint.Style.FILL
            canvas.drawText(el.text, el.x, el.y, paint)
        }
        drawFrame(canvas)
    }

    private fun drawFrame(canvas: Canvas) {
        when (frameType) {
            "white" -> {
                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 14f
                canvas.drawRect(7f, 7f, width - 7f, height - 7f, paint)
            }
            "gradient" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 16f
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#22C55E"), Color.parseColor("#A855F7"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(8f, 8f, width - 8f, height - 8f, paint)
                paint.shader = null
            }
        }
    }

    private fun textWidth(el: TextElement): Float {
        paint.textSize = el.size
        return paint.measureText(el.text)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (i in elements.indices.reversed()) {
                    val el = elements[i]
                    val w = textWidth(el)
                    val h = el.size
                    if (event.x >= el.x - 10 && event.x <= el.x + w + 10 &&
                        event.y >= el.y - h && event.y <= el.y + h
                    ) {
                        dragTarget = el
                        dragOffsetX = event.x - el.x
                        dragOffsetY = event.y - el.y
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                dragTarget?.let {
                    it.x = event.x - dragOffsetX
                    it.y = event.y - dragOffsetY
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragTarget = null
            }
        }
        return super.onTouchEvent(event)
    }

    fun exportBitmap(): Bitmap? {
        val bmp = baseBitmap ?: return null
        val w = if (width > 0) width else bmp.width
        val h = if (height > 0) height else bmp.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(bmp, null, Rect(0, 0, w, h), paint)
        for (el in elements) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = el.color
            paint.textSize = el.size
            c.drawText(el.text, el.x, el.y, paint)
        }
        drawFrame(c)
        return out
    }
}
