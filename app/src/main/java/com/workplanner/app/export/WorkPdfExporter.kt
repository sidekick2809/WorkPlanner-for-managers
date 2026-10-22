package com.workplanner.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.workplanner.app.data.Person
import com.workplanner.app.data.Task
import com.workplanner.app.data.WorkSnapshot
import com.workplanner.app.ui.common.initialsOf
import com.workplanner.app.ui.common.isOverdue
import com.workplanner.app.ui.theme.avatarColors
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the printable team report. Everything is drawn with Android's native
 * PdfDocument API so the project stays free of extra PDF dependencies.
 */
object WorkPdfExporter {

    private const val TAG = "WorkPdfExporter"
    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    private const val CONTENT_BOTTOM = PAGE_HEIGHT - 62f

    private const val PAPER = 0xFFFFFCF4.toInt()
    private const val INK = 0xFF2A2A22.toInt()
    private const val INK_SOFT = 0xFF5F604F.toInt()
    private const val OLIVE = 0xFF4E5F2E.toInt()
    private const val LEAF = 0xFF7C8F51.toInt()
    private const val LINE = 0xFFD9D3C0.toInt()

    fun export(context: Context, snapshot: WorkSnapshot): File? = runCatching {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }

        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.forLanguageTag("tr-TR"))
            .format(now)
        val file = File(directory, "WorkPlanner-Ekip-Raporu-$stamp.pdf")

        val document = PdfDocument()
        try {
            val writer = ReportWriter(document, now)
            writer.write(snapshot)
            writer.finish()
            FileOutputStream(file).use { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
        file
    }.onFailure {
        Log.e(TAG, "PDF raporu oluşturulamadı", it)
    }.getOrNull()

    private class ReportWriter(
        private val document: PdfDocument,
        private val generatedAt: Date
    ) {
        private val locale = Locale.forLanguageTag("tr-TR")
        private val reportDate = SimpleDateFormat("d MMMM yyyy", locale).format(generatedAt)
        private val dateTimeFormat = SimpleDateFormat("d MMM yyyy • HH:mm", locale)

        private val headerPaint = paint(8.5f, OLIVE, Typeface.BOLD)
        private val titlePaint = paint(21f, INK, Typeface.BOLD)
        private val datePaint = paint(9f, INK_SOFT)
        private val metaPaint = paint(9f, INK_SOFT)
        private val namePaint = paint(14f, INK, Typeface.BOLD)
        private val sectionPaint = paint(9.5f, OLIVE, Typeface.BOLD)
        private val taskTitlePaint = paint(11f, INK)
        private val taskTitleDonePaint = paint(11f, INK_SOFT)
        private val taskBodyPaint = paint(9.2f, INK_SOFT)
        private val taskMetaPaint = paint(8.5f, INK_SOFT)
        private val emptyPaint = paint(9.5f, INK_SOFT)
        private val footerPaint = paint(8f, INK_SOFT)
        private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LINE
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        private var y = 0f
        private var activePerson: String? = null

        fun write(snapshot: WorkSnapshot) {
            newPage()
            if (snapshot.people.isEmpty()) {
                ensureSpace(32f)
                canvas?.drawText("Henüz kimse eklenmemiş.", MARGIN, y + 12f, emptyPaint)
                y += 28f
            }
            snapshot.people.forEach { person ->
                drawPerson(
                    person = person,
                    pending = snapshot.pendingTasks(person.id),
                    done = snapshot.doneTasks(person.id)
                )
            }
        }

        fun finish() {
            closePage()
        }

        private fun newPage() {
            closePage()
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            canvas = newPage.canvas
            newPage.canvas.drawColor(PAPER)
            y = MARGIN
            drawPageHeader(newPage.canvas)
            activePerson?.let { name ->
                newPage.canvas.drawText("$name (devam)", MARGIN, y + 8f, sectionPaint)
                y += 20f
            }
        }

        private fun closePage() {
            val currentPage = page ?: return
            val currentCanvas = canvas ?: return
            drawFooter(currentCanvas)
            document.finishPage(currentPage)
            page = null
            canvas = null
        }

        private fun drawPageHeader(c: Canvas) {
            c.drawText("WORK PLANNER", MARGIN, MARGIN + 8f, headerPaint)
            c.drawText(
                reportDate,
                PAGE_WIDTH - MARGIN - datePaint.measureText(reportDate),
                MARGIN + 8f,
                datePaint
            )
            c.drawText("Ekip Görev Raporu", MARGIN, MARGIN + 36f, titlePaint)
            c.drawLine(MARGIN, MARGIN + 48f, PAGE_WIDTH - MARGIN, MARGIN + 48f, dividerPaint)
            y = MARGIN + 64f
        }

        private fun drawFooter(c: Canvas) {
            val footerY = PAGE_HEIGHT - 26f
            c.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, dividerPaint)
            c.drawText("WorkPlanner · Ekip görev raporu", MARGIN, footerY, footerPaint)
            val pageText = pageNumber.toString()
            c.drawText(
                pageText,
                PAGE_WIDTH - MARGIN - footerPaint.measureText(pageText),
                footerY,
                footerPaint
            )
        }

        private fun ensureSpace(height: Float) {
            if (y + height > CONTENT_BOTTOM) newPage()
        }

        private fun drawPerson(person: Person, pending: List<Task>, done: List<Task>) {
            ensureSpace(82f)
            val c = canvas ?: return

            val colors = avatarColors(person.colorIndex)
            val centerX = MARGIN + 15f
            val centerY = y + 15f
            c.drawCircle(
                centerX,
                centerY,
                15f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.container.toArgb() }
            )
            val initials = initialsOf(person.name)
            val initialsPaint = paint(10f, colors.content.toArgb(), Typeface.BOLD)
            c.drawText(
                initials,
                centerX - initialsPaint.measureText(initials) / 2f,
                centerY - (initialsPaint.ascent() + initialsPaint.descent()) / 2f,
                initialsPaint
            )

            c.drawText(person.name, MARGIN + 40f, y + 12f, namePaint)
            c.drawText(
                "${pending.size} bekleyen · ${done.size} tamamlanan",
                MARGIN + 40f,
                y + 27f,
                metaPaint
            )
            y += 42f
            activePerson = person.name

            if (pending.isEmpty() && done.isEmpty()) {
                ensureSpace(26f)
                canvas?.drawText("Henüz iş yok.", MARGIN + 2f, y + 10f, emptyPaint)
                y += 26f
            }

            if (pending.isNotEmpty()) {
                drawSection("Bekleyen işler", pending.size)
                pending.forEach { drawTask(it, done = false) }
            }
            if (done.isNotEmpty()) {
                drawSection("Tamamlanan işler", done.size)
                done.forEach { drawTask(it, done = true) }
            }

            activePerson = null
            if (y + 12f < CONTENT_BOTTOM) {
                y += 8f
                canvas?.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                y += 16f
            }
        }

        private fun drawSection(title: String, count: Int) {
            ensureSpace(24f)
            canvas?.drawText("$title  ·  $count", MARGIN + 2f, y + 10f, sectionPaint)
            y += 22f
        }

        private fun drawTask(task: Task, done: Boolean) {
            val textWidth = (PAGE_WIDTH - MARGIN * 2f - 24f).toInt()
            val titleLayout = layout(
                task.title,
                if (done) taskTitleDonePaint else taskTitlePaint,
                textWidth,
                maxLines = 2
            )
            val summaryLayout = task.summary.takeIf { it.isNotBlank() }?.let {
                layout(it, taskBodyPaint, textWidth, maxLines = 2)
            }
            val metaLayout = layout(buildMeta(task, done), taskMetaPaint, textWidth, maxLines = 2)

            val blockHeight = 10f +
                titleLayout.height + 2f +
                (summaryLayout?.height?.plus(2f) ?: 0f) +
                metaLayout.height + 10f
            ensureSpace(blockHeight)

            val c = canvas ?: return
            drawMarker(c, MARGIN + 7f, y + 7f, done)
            val textX = MARGIN + 22f

            c.save()
            c.translate(textX, y)
            titleLayout.draw(c)
            c.restore()
            y += titleLayout.height + 2f

            summaryLayout?.let {
                c.save()
                c.translate(textX, y)
                it.draw(c)
                c.restore()
                y += it.height + 2f
            }

            c.save()
            c.translate(textX, y)
            metaLayout.draw(c)
            c.restore()
            y += metaLayout.height + 12f
        }

        private fun drawMarker(c: Canvas, x: Float, centerY: Float, done: Boolean) {
            if (done) {
                c.drawCircle(
                    x,
                    centerY,
                    5.5f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LEAF }
                )
                val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                c.drawLine(x - 2.6f, centerY, x - 0.6f, centerY + 2f, check)
                c.drawLine(x - 0.6f, centerY + 2f, x + 3f, centerY - 2.4f, check)
            } else {
                val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = OLIVE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.1f
                }
                c.drawCircle(x, centerY, 5f, outline)
            }
        }

        private fun buildMeta(task: Task, done: Boolean): String {
            if (done) {
                return "Tamamlandı · ${dateTimeFormat.format(Date(task.completedAt ?: task.createdAt))}"
            }
            val parts = mutableListOf("Bekliyor")
            if (task.progress > 0) parts += "%${task.progress}"
            task.reminderAt?.let { reminder ->
                parts += if (isOverdue(reminder, false)) {
                    "Hatırlatıcı gecikti · ${dateTimeFormat.format(Date(reminder))}"
                } else {
                    "Hatırlatıcı · ${dateTimeFormat.format(Date(reminder))}"
                }
            }
            return parts.joinToString("  ·  ")
        }

        private fun layout(
            text: String,
            paint: TextPaint,
            width: Int,
            maxLines: Int
        ): StaticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        private fun paint(size: Float, color: Int, style: Int = Typeface.NORMAL): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create("sans-serif", style)
            }
    }
}
