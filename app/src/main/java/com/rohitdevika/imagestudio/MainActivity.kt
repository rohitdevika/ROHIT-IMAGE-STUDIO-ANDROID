
package com.rohitdevika.imagestudio

import android.app.*
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentValues
import android.view.*
import android.widget.*
import kotlin.math.min
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var editor: EditorView
    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(12, 17, 31)
        window.navigationBarColor = Color.rgb(12, 17, 31)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 17, 31))
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 12, 12)
            setBackgroundColor(Color.rgb(20, 29, 49))
        }

        val title = TextView(this).apply {
            text = "ROHIT IMAGE STUDIO"
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        header.addView(title, LinearLayout.LayoutParams(0, 48, 1f))
        header.addView(button("↶") { editor.undo() })
        header.addView(button("↷") { editor.redo() })
        header.addView(button("SAVE") { saveImage() })

        root.addView(header)

        editor = EditorView()
        root.addView(
            editor,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            ).apply {
                setMargins(8, 8, 8, 8)
            }
        )

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 8, 5, 8)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(20, 29, 49))
        }

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tools)
        }

        val actions = listOf(
            "Photo" to { openPhoto() },
            "Text" to { addText() },
            "Sticker" to { addSticker() },
            "Color" to { changeColor() },
            "Size" to { changeSize() },
            "Rotate" to { editor.rotatePhoto() },
            "B&W" to { editor.toggleGray() },
            "Delete" to { editor.deleteText() }
        )

        actions.forEach { (name, action) ->
            tools.addView(toolButton(name, action))
        }

        root.addView(scroll)

        val footer = TextView(this).apply {
            text = "Select • Move • Edit • Export"
            gravity = Gravity.CENTER
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(8, 10, 8, 10)
            setBackgroundColor(Color.rgb(20, 29, 49))
        }

        root.addView(footer)
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 8, 12, 8)
            setOnClickListener { action() }
        }

    private fun toolButton(label: String, action: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(16, 14, 16, 14)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(38, 53, 82))
                cornerRadius = 18f
            }
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.setMargins(4, 0, 4, 0)
            layoutParams = lp
            setOnClickListener { action() }
        }

    private fun openPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_IMAGE)
    }

    @Deprecated("Deprecated callback kept for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use {
                    val bitmap = BitmapFactory.decodeStream(it)
                    if (bitmap != null) editor.setPhoto(bitmap)
                }
            }
        }
    }

    private fun addText() {
        val input = EditText(this)
        input.hint = "Write your text"

        AlertDialog.Builder(this)
            .setTitle("Add Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text.toString()
                if (value.isNotBlank()) editor.addText(value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSticker() {
        val stickers = arrayOf("★", "♥", "●", "◆", "✦", "⚡")

        AlertDialog.Builder(this)
            .setTitle("Choose Sticker")
            .setItems(stickers) { _, which ->
                editor.addText(stickers[which])
            }
            .show()
    }

    private fun changeColor() {
        val colors = intArrayOf(
            Color.WHITE,
            Color.YELLOW,
            Color.RED,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.BLACK
        )

        val names = arrayOf(
            "White", "Yellow", "Red",
            "Cyan", "Green", "Pink", "Black"
        )

        AlertDialog.Builder(this)
            .setTitle("Text Color")
            .setItems(names) { _, which ->
                editor.setColor(colors[which])
            }
            .show()
    }

    private fun changeSize() {
        val seek = SeekBar(this).apply {
            max = 100
            progress = 35
        }

        AlertDialog.Builder(this)
            .setTitle("Text Size")
            .setView(seek)
            .setPositiveButton("Apply") { _, _ ->
                editor.setSize(20f + seek.progress * 1.5f)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveImage() {
        val bitmap = editor.exportBitmap() ?: run {
            Toast.makeText(
                this, "First open a photo", Toast.LENGTH_SHORT
            ).show()
            return
        }

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "RohitStudio_${System.currentTimeMillis()}.png"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/Rohit Image Studio"
            )
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Toast.makeText(
                this, "Image saved successfully",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    inner class EditorView : View(this@MainActivity) {

        private data class Layer(
            var text: String,
            var x: Float,
            var y: Float,
            var size: Float = 60f,
            var color: Int = Color.WHITE
        )

        private var photo: Bitmap? = null
        private val layers = mutableListOf<Layer>()
        private var selected = -1
        private var gray = false
        private var rotation = 0f
        private var downX = 0f
        private var downY = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun setPhoto(bitmap: Bitmap) {
            photo = bitmap
            layers.clear()
            selected = -1
            gray = false
            rotation = 0f
            invalidate()
        }

        fun addText(value: String) {
            val b = photo ?: return
            layers.add(
                Layer(value, b.width / 2f, b.height / 2f)
            )
            selected = layers.lastIndex
            invalidate()
        }

        fun setColor(color: Int) {
            if (selected >= 0) {
                layers[selected].color = color
                invalidate()
            }
        }

        fun setSize(size: Float) {
            if (selected >= 0) {
                layers[selected].size = size
                invalidate()
            }
        }

        fun deleteText() {
            if (selected >= 0) {
                layers.removeAt(selected)
                selected = -1
                invalidate()
            }
        }

        fun rotatePhoto() {
            rotation = (rotation + 90f) % 360f
            invalidate()
        }

        fun toggleGray() {
            gray = !gray
            invalidate()
        }

        fun undo() {
            Toast.makeText(
                this@MainActivity,
                "Undo history will be added in the next version",
                Toast.LENGTH_SHORT
            ).show()
        }

        fun redo() {
            Toast.makeText(
                this@MainActivity,
                "Redo history will be added in the next version",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(7, 12, 24))

            val b = photo
            if (b == null) {
                paint.color = Color.LTGRAY
                paint.textSize = 16f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    "Open a photo to start editing",
                    width / 2f, height / 2f, paint
                )
                return
            }

            val scale = min(
                width.toFloat() / b.width,
                height.toFloat() / b.height
            )

            val dw = b.width * scale
            val dh = b.height * scale
            val ox = (width - dw) / 2f
            val oy = (height - dh) / 2f

            val save = canvas.save()
            canvas.rotate(
                rotation, width / 2f, height / 2f
            )

            paint.colorFilter = if (gray) {
                ColorMatrixColorFilter(
                    ColorMatrix().apply { setSaturation(0f) }
                )
            } else null

            canvas.drawBitmap(
                b, null,
                RectF(ox, oy, ox + dw, oy + dh),
                paint
            )
            paint.colorFilter = null
            canvas.restoreToCount(save)

            layers.forEachIndexed { index, layer ->
                val x = ox + layer.x * scale
                val y = oy + layer.y * scale

                paint.color = layer.color
                paint.textSize = layer.size * scale
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.CENTER
                paint.setShadowLayer(3f, 2f, 2f, Color.BLACK)

                canvas.drawText(layer.text, x, y, paint)
                paint.clearShadowLayer()

                if (index == selected) {
                    val w = paint.measureText(layer.text)
                    paint.color = Color.CYAN
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawRect(
                        x - w / 2 - 8,
                        y - layer.size * scale,
                        x + w / 2 + 8,
                        y + 10,
                        paint
                    )
                    paint.style = Paint.Style.FILL
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val b = photo ?: return true
            val scale = min(
                width.toFloat() / b.width,
                height.toFloat() / b.height
            )
            val ox = (width - b.width * scale) / 2f
            val oy = (height - b.height * scale) / 2f

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    selected = -1

                    for (i in layers.indices.reversed()) {
                        val x = ox + layers[i].x * scale
                        val y = oy + layers[i].y * scale

                        if (abs(event.x - x) < 140 &&
                            abs(event.y - y) < 100) {
                            selected = i
                            break
                        }
                    }
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (selected >= 0) {
                        layers[selected].x =
                            ((event.x - ox) / scale)
                                .coerceIn(0f, b.width.toFloat())

                        layers[selected].y =
                            ((event.y - oy) / scale)
                                .coerceIn(0f, b.height.toFloat())

                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    performClick()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        fun exportBitmap(): Bitmap? {
            val b = photo ?: return null
            val output = Bitmap.createBitmap(
                b.width, b.height, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            canvas.rotate(
                rotation, b.width / 2f, b.height / 2f
            )

            if (gray) {
                p.colorFilter = ColorMatrixColorFilter(
                    ColorMatrix().apply { setSaturation(0f) }
                )
            }

            canvas.drawBitmap(b, 0f, 0f, p)
            p.colorFilter = null

            layers.forEach { layer ->
                p.color = layer.color
                p.textSize = layer.size
                p.typeface = Typeface.DEFAULT_BOLD
                p.textAlign = Paint.Align.CENTER
                p.setShadowLayer(4f, 2f, 2f, Color.BLACK)
                canvas.drawText(
                    layer.text, layer.x, layer.y, p
                )
                p.clearShadowLayer()
            }

            return output
        }
    }
}
