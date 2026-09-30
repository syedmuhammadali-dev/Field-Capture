package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoStorageService(private val context: Context) {
    private companion object {
        const val MAX_PHOTO_DIMENSION = 1600
        const val MAX_PHOTO_BYTES = 3 * 1024 * 1024
    }

    private val ticketsDir: File
        get() {
            val dir = File(context.filesDir, "delivery_tickets")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /** Stores an orientation-corrected JPEG below the backend upload size limit. */
    fun saveImageFromUri(sourceUri: Uri): String {
        val fileName = "ticket_${UUID.randomUUID()}.jpg"
        val destinationFile = File(ticketsDir, fileName)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw IOException("Could not open selected photo")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Selected file is not a supported image")
        }

        val orientation = context.contentResolver.openInputStream(sourceUri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_DIMENSION)
        }
        val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IOException("Could not decode selected photo")

        var bitmap = orientBitmap(decodedBitmap, orientation)
        if (bitmap !== decodedBitmap) decodedBitmap.recycle()

        val scale = minOf(
            1f,
            MAX_PHOTO_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
        )
        if (scale < 1f) {
            val resized = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (resized !== bitmap) bitmap.recycle()
            bitmap = resized
        }

        val compressed = ByteArrayOutputStream()
        var quality = 85
        try {
            while (true) {
                compressed.reset()
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, compressed)) {
                    throw IOException("Could not compress selected photo")
                }
                if (compressed.size() <= MAX_PHOTO_BYTES) break

                if (quality > 55) {
                    quality -= 10
                } else {
                    val smaller = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * 0.8f).toInt().coerceAtLeast(1),
                        (bitmap.height * 0.8f).toInt().coerceAtLeast(1),
                        true
                    )
                    if (smaller === bitmap) {
                        throw IOException("Photo could not be compressed below 3 MB")
                    }
                    bitmap.recycle()
                    bitmap = smaller
                    quality = 80
                }
            }

            FileOutputStream(destinationFile).use { compressed.writeTo(it) }
        } finally {
            bitmap.recycle()
        }

        return destinationFile.absolutePath
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (maxOf(width / sampleSize, height / sampleSize) > maxDimension * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun orientBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Generates a realistic delivery ticket document bitmap and saves it locally.
     * Useful for on-site demo, emulator testing, and automated ticket generation.
     */
    fun generateDeliveryTicket(
        supplier: String,
        poNumber: String,
        materialNotes: String
    ): String {
        val width = 800
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Aged ticket paper
        canvas.drawColor(Color.rgb(250, 249, 246))

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Draw outer border and header banner
        paint.color = Color.rgb(30, 58, 95)
        canvas.drawRect(30f, 30f, (width - 30).toFloat(), 150f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FIELD MATERIAL DELIVERY TICKET", 60f, 100f, paint)

        // Subheader info
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date())

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated at Job Site • Delivery Confirmation", 60f, 135f, paint)

        // Ticket Details box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(50f, 180f, (width - 50).toFloat(), 480f, paint)

        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(50f, 180f, (width - 50).toFloat(), 480f, paint)
        paint.style = Paint.Style.FILL

        // Detail text
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER:", 80f, 240f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(supplier.ifBlank { "Apex Construction Materials Ltd." }, 260f, 240f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("P.O. NUMBER:", 80f, 300f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(poNumber.ifBlank { "PO-9824-TX" }, 260f, 300f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TIMESTAMP:", 80f, 360f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(dateStr, 260f, 360f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STATUS:", 80f, 420f, paint)
        paint.color = Color.rgb(22, 163, 74)
        canvas.drawText("RECEIVED ON-SITE", 260f, 420f, paint)

        // Itemized Materials Table
        paint.color = Color.rgb(30, 58, 95)
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DELIVERY MANIFEST / ITEMS", 50f, 530f, paint)

        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(50f, 545f, (width - 50).toFloat(), 545f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val sampleLines = listOf(
            "• Item 1: Grade 60 #4 Rebar - 120 bundles (Verified)",
            "• Item 2: Ready-Mix Concrete Slump Test Verified (4.5\")",
            "• Item 3: Portland Cement Type I/II - 40 bags",
            "• Manifest Notes: " + materialNotes.ifBlank { "Inspected at west gate entrance" }
        )

        var y = 590f
        for (line in sampleLines) {
            canvas.drawText(line, 60f, y, paint)
            y += 45f
        }

        // Signature section
        y = 860f
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 18f
        canvas.drawLine(80f, y, 350f, y, paint)
        canvas.drawText("Foreman Signature: Verified in Field", 80f, y + 30f, paint)

        canvas.drawLine(450f, y, 720f, y, paint)
        canvas.drawText("Driver Signature: Deliverer Confirmed", 450f, y + 30f, paint)

        // Barcode decorative lines
        val barcodeY = 960f
        paint.color = Color.rgb(15, 23, 42)
        paint.strokeWidth = 3f
        var bx = 120f
        val barcodePattern = intArrayOf(2, 4, 1, 3, 2, 5, 1, 4, 3, 2, 6, 2, 1, 4, 3, 1, 5, 2, 3, 4, 2, 1, 4, 2, 5, 3)
        for (w in barcodePattern) {
            paint.strokeWidth = (w * 2).toFloat()
            canvas.drawLine(bx, barcodeY, bx, barcodeY + 60f, paint)
            bx += (w * 2 + 6).toFloat()
        }

        val fileName = "ticket_${UUID.randomUUID()}.jpg"
        val destinationFile = File(ticketsDir, fileName)
        FileOutputStream(destinationFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        return destinationFile.absolutePath
    }

    fun getFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }
}
