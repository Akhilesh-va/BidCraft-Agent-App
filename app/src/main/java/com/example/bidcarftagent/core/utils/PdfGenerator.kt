package com.example.bidcarftagent.core.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generatePdf(title: String, content: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Draw Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(title, 50f, 60f, paint)

        // Draw Content
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val x = 50f
        var y = 100f
        val maxWidth = 500f // Allow some margins
        
        // Simple word wrap
        val words = content.split(" ")
        var currentLine = ""
        
        for (word in words) {
            val potentialLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(potentialLine)
            
            if (textWidth > maxWidth) {
                // Draw current line and start new one
                canvas.drawText(currentLine, x, y, paint)
                y += 20f // Line height
                currentLine = word
                
                // Check if we need a new page (simplified: just stops drawing if full)
                if (y > 800f) break 
            } else {
                currentLine = potentialLine
            }
        }
        // Draw last line
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, y, paint)
        }

        pdfDocument.finishPage(page)

        // Save file to cache directory
        val fileName = "BidCraft_Proposal_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }
}
