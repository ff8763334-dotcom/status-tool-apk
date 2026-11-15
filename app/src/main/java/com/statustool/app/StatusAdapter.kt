package com.statustool.app

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView

data class StatusItem(val doc: DocumentFile, val isVideo: Boolean)

class StatusAdapter(
    private val context: Context,
    private val items: List<StatusItem>
) : RecyclerView.Adapter<StatusAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.thumb)
        val videoLabel: TextView = view.findViewById(R.id.videoLabel)
        val saveBtn: TextView = view.findViewById(R.id.saveBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_status, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.videoLabel.visibility = if (item.isVideo) android.view.View.VISIBLE else android.view.View.GONE
        holder.thumb.setImageBitmap(null)
        holder.thumb.setBackgroundColor(0xFF2B2B30.toInt())

        if (!item.isVideo) {
            try {
                context.contentResolver.openInputStream(item.doc.uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bmp = BitmapFactory.decodeStream(input, null, opts)
                    holder.thumb.setImageBitmap(bmp)
                }
            } catch (e: Exception) { }
        }

        holder.saveBtn.setOnClickListener {
            saveToGallery(item)
        }
    }

    private fun saveToGallery(item: StatusItem) {
        try {
            val resolver: ContentResolver = context.contentResolver
            val name = item.doc.name ?: ("status_" + System.currentTimeMillis())
            val values = ContentValues()
            val collection: Uri
            if (item.isVideo) {
                values.put(MediaStore.Video.Media.DISPLAY_NAME, name)
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= 29) {
                    values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/StatusTool")
                }
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) {
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StatusTool")
                }
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val newUri = resolver.insert(collection, values) ?: return
            resolver.openOutputStream(newUri)?.use { out ->
                resolver.openInputStream(item.doc.uri)?.use { input ->
                    input.copyTo(out)
                }
            }
            Toast.makeText(context, "تم الحفظ: $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذّر الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
