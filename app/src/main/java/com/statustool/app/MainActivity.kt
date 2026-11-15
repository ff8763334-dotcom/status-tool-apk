package com.statustool.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var sectionDeco: LinearLayout
    private lateinit var sectionSaver: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var gridRecycler: RecyclerView
    private var selectedColor: Int = Color.WHITE

    private val mediaExt = listOf("jpg", "jpeg", "png", "webp", "mp4")

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) drawingView.setImage(bmp)
            }
        }
    }

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadStatusFolder(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        sectionDeco = findViewById(R.id.sectionDeco)
        sectionSaver = findViewById(R.id.sectionSaver)
        emptyText = findViewById(R.id.emptyText)
        gridRecycler = findViewById(R.id.gridRecycler)
        gridRecycler.layoutManager = GridLayoutManager(this, 3)

        findViewById<Button>(R.id.btnTabDeco).setOnClickListener {
            sectionDeco.visibility = android.view.View.VISIBLE
            sectionSaver.visibility = android.view.View.GONE
        }
        findViewById<Button>(R.id.btnTabSaver).setOnClickListener {
            sectionDeco.visibility = android.view.View.GONE
            sectionSaver.visibility = android.view.View.VISIBLE
        }

        findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val editText = findViewById<EditText>(R.id.editText)
        val sizeSeek = findViewById<SeekBar>(R.id.sizeSeek)

        findViewById<Button>(R.id.colorWhite).setOnClickListener { selectedColor = Color.WHITE }
        findViewById<Button>(R.id.colorBlack).setOnClickListener { selectedColor = Color.BLACK }
        findViewById<Button>(R.id.colorRed).setOnClickListener { selectedColor = Color.parseColor("#EF4444") }
        findViewById<Button>(R.id.colorYellow).setOnClickListener { selectedColor = Color.parseColor("#FACC15") }
        findViewById<Button>(R.id.colorGreen).setOnClickListener { selectedColor = Color.parseColor("#22C55E") }

        findViewById<Button>(R.id.btnAddText).setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                if (!drawingView.hasImage()) {
                    Toast.makeText(this, "اختر صورة أولاً", Toast.LENGTH_SHORT).show()
                } else {
                    val size = (sizeSeek.progress + 16).toFloat()
                    drawingView.addText(text, selectedColor, size)
                    editText.setText("")
                }
            }
        }

        val emojiBar = findViewById<LinearLayout>(R.id.emojiBar)
        val emojis = listOf("❤️", "🔥", "😂", "😍", "👍", "🎉", "⭐", "😎", "💯", "✨")
        for (em in emojis) {
            val btn = Button(this)
            btn.text = em
            btn.textSize = 20f
            btn.setPadding(20, 10, 20, 10)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 8
            btn.layoutParams = lp
            btn.setOnClickListener {
                if (drawingView.hasImage()) drawingView.addText(em, Color.BLACK, 40f)
            }
            emojiBar.addView(btn)
        }

        val frames = listOf("بدون", "إطار أبيض", "إطار متدرّج")
        val frameValues = listOf("none", "white", "gradient")
        val frameSpinner = findViewById<Spinner>(R.id.frameSpinner)
        frameSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, frames)
        frameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                drawingView.frameType = frameValues[pos]
                drawingView.invalidate()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnUndo).setOnClickListener { drawingView.undo() }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            val bmp = drawingView.exportBitmap()
            if (bmp == null) {
                Toast.makeText(this, "اختر صورة أولاً", Toast.LENGTH_SHORT).show()
            } else {
                val name = "status_" + System.currentTimeMillis() + ".jpg"
                val values = android.content.ContentValues()
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StatusTool")
                }
                val uri = contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    Toast.makeText(this, "تم الحفظ في المعرض ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnPickFolder).setOnClickListener {
            pickFolderLauncher.launch(null)
        }
    }

    private fun loadStatusFolder(treeUri: Uri) {
        val root = DocumentFile.fromTreeUri(this, treeUri)
        val items = mutableListOf<StatusItem>()
        if (root != null) {
            for (f in root.listFiles()) {
                val fname = f.name ?: continue
                if (fname.startsWith(".")) continue
                val lower = fname.lowercase()
                val ext = lower.substringAfterLast('.', "")
                if (ext in mediaExt) {
                    items.add(StatusItem(f, ext == "mp4"))
                }
            }
        }
        emptyText.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        if (items.isEmpty()) {
            emptyText.text = "لا توجد وسائط حالة في هذا المجلد الآن"
        }
        gridRecycler.adapter = StatusAdapter(this, items)
    }
}
