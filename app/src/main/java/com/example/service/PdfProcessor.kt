package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.model.ExtractionPreset
import com.example.data.model.SamplePdfInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfProcessor(private val context: Context) {

    data class PdfMetadata(
        val fileName: String,
        val pageCount: Int,
        val fileSizeBytes: Long,
        val tempFilePath: String
    )

    suspend fun inspectPdf(uri: Uri): PdfMetadata = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri)
        val tempFile = copyUriToTempFile(uri, fileName)
        val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        val pageCount = renderer.pageCount
        renderer.close()
        fileDescriptor.close()

        PdfMetadata(
            fileName = fileName,
            pageCount = pageCount,
            fileSizeBytes = tempFile.length(),
            tempFilePath = tempFile.absolutePath
        )
    }

    suspend fun renderPagesToBitmaps(filePath: String, maxPages: Int = 5): List<Bitmap> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext emptyList()

        val bitmaps = mutableListOf<Bitmap>()
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        val count = minOf(renderer.pageCount, maxPages)

        for (i in 0 until count) {
            val page = renderer.openPage(i)
            // Render at 2x crisp resolution (e.g., standard A4 595x842 -> 1190x1684)
            val scale = 2.0f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }

        renderer.close()
        fileDescriptor.close()
        bitmaps
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun copyUriToTempFile(uri: Uri, fileName: String): File {
        val tempDir = File(context.cacheDir, "pdf_temp").apply { mkdirs() }
        val sanitized = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val tempFile = File(tempDir, "${System.currentTimeMillis()}_$sanitized")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun getFileName(uri: Uri): String {
        var name = "document.pdf"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    // ==========================================
    // Sample PDF Generator for Instant Testing
    // ==========================================

    val samplePdfs: List<SamplePdfInfo> = listOf(
        SamplePdfInfo(
            id = "sample_invoice",
            title = "Global Tech Invoice #INV-8092",
            subtitle = "SaaS licenses, server hosting & developer services with tax breakdown",
            category = "Invoice",
            pageCount = 1,
            defaultPreset = ExtractionPreset.INVOICE_RECEIPT,
            estimatedRows = 5
        ),
        SamplePdfInfo(
            id = "sample_financial",
            title = "Q3 Commercial Banking Statement",
            subtitle = "Checking account transactions, deposits, wire transfers, running balances",
            category = "Banking",
            pageCount = 2,
            defaultPreset = ExtractionPreset.FINANCIAL_STATEMENT,
            estimatedRows = 12
        ),
        SamplePdfInfo(
            id = "sample_inventory",
            title = "Warehouse Inventory & Price Master",
            subtitle = "SKU catalog, stock quantities, unit costs, retail pricing, margins",
            category = "Inventory",
            pageCount = 1,
            defaultPreset = ExtractionPreset.INVENTORY_CATALOG,
            estimatedRows = 8
        ),
        SamplePdfInfo(
            id = "sample_timesheet",
            title = "Engineering Staff Timesheet & Payroll",
            subtitle = "Bi-weekly contractor hours, hourly billing rates, overtime, gross pay",
            category = "Payroll",
            pageCount = 1,
            defaultPreset = ExtractionPreset.TIMESHEET_PAYROLL,
            estimatedRows = 7
        )
    )

    suspend fun generateSamplePdf(sampleId: String): File = withContext(Dispatchers.IO) {
        val samplesDir = File(context.cacheDir, "sample_pdfs").apply { mkdirs() }
        val targetFile = File(samplesDir, "$sampleId.pdf")

        // Reuse if already generated
        if (targetFile.exists() && targetFile.length() > 500) {
            return@withContext targetFile
        }

        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (sampleId) {
            "sample_invoice" -> createSampleInvoicePdf(pdfDocument, paint, textPaint)
            "sample_financial" -> createSampleFinancialPdf(pdfDocument, paint, textPaint)
            "sample_inventory" -> createSampleInventoryPdf(pdfDocument, paint, textPaint)
            "sample_timesheet" -> createSampleTimesheetPdf(pdfDocument, paint, textPaint)
            else -> createSampleInvoicePdf(pdfDocument, paint, textPaint)
        }

        FileOutputStream(targetFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        targetFile
    }

    private fun createSampleInvoicePdf(doc: PdfDocument, paint: Paint, textPaint: Paint) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Header Background Banner
        paint.color = Color.parseColor("#0F766E")
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = true
        canvas.drawText("ACME CLOUD SOLUTIONS INC.", 40f, 50f, textPaint)

        textPaint.textSize = 12f
        textPaint.isFakeBoldText = false
        canvas.drawText("INVOICE #INV-2026-8092  |  DATE: AUG 24, 2026", 40f, 75f, textPaint)

        // Customer Details Box
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(40f, 120f, 555f, 190f, 8f, 8f, paint)

        textPaint.color = Color.parseColor("#0F172A")
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        canvas.drawText("BILLED TO:", 55f, 145f, textPaint)
        textPaint.isFakeBoldText = false
        canvas.drawText("Nexus Digital Systems LLC  |  Attn: Accounts Payable", 55f, 165f, textPaint)
        canvas.drawText("Payment Terms: Net 30 Days  |  Due Date: Sep 23, 2026", 55f, 180f, textPaint)

        // Table Header
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(40f, 210f, 555f, 240f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 11f
        textPaint.isFakeBoldText = true
        canvas.drawText("ITEM DESCRIPTION", 50f, 230f, textPaint)
        canvas.drawText("QTY", 320f, 230f, textPaint)
        canvas.drawText("UNIT PRICE", 380f, 230f, textPaint)
        canvas.drawText("TAX", 460f, 230f, textPaint)
        canvas.drawText("AMOUNT", 505f, 230f, textPaint)

        // Table Rows
        val items = listOf(
            arrayOf("Enterprise Cloud Tier - Annual (US-East)", "2", "$1,450.00", "8%", "$2,900.00"),
            arrayOf("Database High-Availability Clustering Addon", "1", "$680.00", "8%", "$680.00"),
            arrayOf("AI Processing Pipeline (10M Token Pack)", "5", "$120.00", "0%", "$600.00"),
            arrayOf("Dedicated Support SLA (24/7 Priority)", "1", "$450.00", "8%", "$450.00"),
            arrayOf("API Gateway & Global Load Balancer Setup", "1", "$320.00", "8%", "$320.00")
        )

        var y = 265f
        textPaint.color = Color.parseColor("#1E293B")
        paint.color = Color.parseColor("#E2E8F0")

        for (item in items) {
            textPaint.isFakeBoldText = false
            canvas.drawText(item[0], 50f, y, textPaint)
            canvas.drawText(item[1], 325f, y, textPaint)
            canvas.drawText(item[2], 385f, y, textPaint)
            canvas.drawText(item[3], 465f, y, textPaint)
            textPaint.isFakeBoldText = true
            canvas.drawText(item[4], 500f, y, textPaint)

            canvas.drawLine(40f, y + 12f, 555f, y + 12f, paint)
            y += 35f
        }

        // Summary Totals Box
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(350f, y + 10f, 555f, y + 130f, paint)

        textPaint.color = Color.parseColor("#475569")
        textPaint.isFakeBoldText = false
        canvas.drawText("Subtotal:", 365f, y + 35f, textPaint)
        canvas.drawText("$4,950.00", 490f, y + 35f, textPaint)

        canvas.drawText("Estimated Tax (8%):", 365f, y + 60f, textPaint)
        canvas.drawText("$348.00", 490f, y + 60f, textPaint)

        canvas.drawText("Discount (Code: SAVE50):", 365f, y + 85f, textPaint)
        canvas.drawText("-$50.00", 490f, y + 85f, textPaint)

        textPaint.color = Color.parseColor("#0F766E")
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas.drawText("TOTAL DUE:", 365f, y + 115f, textPaint)
        canvas.drawText("$5,248.00", 480f, y + 115f, textPaint)

        // Footer
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 9f
        textPaint.isFakeBoldText = false
        canvas.drawText("Thank you for your business. For wire transfer: Chase Bank, Routing #021000021, Acct #984210984", 40f, 800f, textPaint)

        doc.finishPage(page)
    }

    private fun createSampleFinancialPdf(doc: PdfDocument, paint: Paint, textPaint: Paint) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        paint.color = Color.parseColor("#1E3A8A")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = true
        canvas.drawText("FIRST NATIONAL COMMERCIAL BANK", 40f, 45f, textPaint)

        textPaint.textSize = 11f
        textPaint.isFakeBoldText = false
        canvas.drawText("Account Statement  |  Period: Aug 01, 2026 - Aug 28, 2026", 40f, 70f, textPaint)

        // Table Header
        paint.color = Color.parseColor("#334155")
        canvas.drawRect(40f, 130f, 555f, 155f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("DATE", 48f, 147f, textPaint)
        canvas.drawText("DESCRIPTION / PAYEE", 120f, 147f, textPaint)
        canvas.drawText("REF #", 310f, 147f, textPaint)
        canvas.drawText("DEBIT (-)", 380f, 147f, textPaint)
        canvas.drawText("CREDIT (+)", 450f, 147f, textPaint)
        canvas.drawText("BALANCE", 510f, 147f, textPaint)

        val txs = listOf(
            arrayOf("2026-08-01", "Beginning Balance", "INIT", "", "", "$18,450.00"),
            arrayOf("2026-08-03", "Stripe Payout - Merchant Sales", "TRX-9481", "", "$4,820.50", "$23,270.50"),
            arrayOf("2026-08-05", "Google Cloud EMEA Platform", "ACH-4819", "$1,240.00", "", "$22,030.50"),
            arrayOf("2026-08-09", "Office Lease - Manhattan Prop", "CHK-1092", "$3,500.00", "", "$18,530.50"),
            arrayOf("2026-08-12", "Client Invoice Wire #8812", "WIRE-019", "", "$9,400.00", "$27,930.50"),
            arrayOf("2026-08-15", "Payroll Direct Deposit Batch", "PAY-8091", "$6,850.00", "", "$21,080.50"),
            arrayOf("2026-08-18", "Amazon Web Services Hosting", "ACH-7721", "$890.20", "", "$20,190.30"),
            arrayOf("2026-08-22", "Enterprise SaaS Subscriptions", "CARD-402", "$340.00", "", "$19,850.30"),
            arrayOf("2026-08-26", "Customer Settlement Stripe", "TRX-9941", "", "$3,110.00", "$22,960.30"),
            arrayOf("2026-08-28", "Interest Yield Paid", "INT-8801", "", "$42.10", "$23,002.40")
        )

        var y = 180f
        paint.color = Color.parseColor("#E2E8F0")

        for (tx in txs) {
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 9.5f
            textPaint.isFakeBoldText = false
            canvas.drawText(tx[0], 48f, y, textPaint)
            canvas.drawText(tx[1], 120f, y, textPaint)
            canvas.drawText(tx[2], 310f, y, textPaint)

            if (tx[3].isNotBlank()) {
                textPaint.color = Color.parseColor("#DC2626")
                canvas.drawText(tx[3], 380f, y, textPaint)
            }
            if (tx[4].isNotBlank()) {
                textPaint.color = Color.parseColor("#16A34A")
                canvas.drawText(tx[4], 450f, y, textPaint)
            }
            textPaint.color = Color.parseColor("#0F172A")
            textPaint.isFakeBoldText = true
            canvas.drawText(tx[5], 505f, y, textPaint)

            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)
            y += 28f
        }

        doc.finishPage(page)
    }

    private fun createSampleInventoryPdf(doc: PdfDocument, paint: Paint, textPaint: Paint) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        paint.color = Color.parseColor("#065F46")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = true
        canvas.drawText("TECHSTORE LOGISTICS & INVENTORY", 40f, 45f, textPaint)

        textPaint.textSize = 11f
        textPaint.isFakeBoldText = false
        canvas.drawText("Master Warehouse Stock & Valuation Report", 40f, 70f, textPaint)

        // Table Header
        paint.color = Color.parseColor("#064E3B")
        canvas.drawRect(40f, 130f, 555f, 155f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("SKU CODE", 48f, 147f, textPaint)
        canvas.drawText("PRODUCT NAME", 130f, 147f, textPaint)
        canvas.drawText("CATEGORY", 300f, 147f, textPaint)
        canvas.drawText("IN STOCK", 385f, 147f, textPaint)
        canvas.drawText("UNIT COST", 445f, 147f, textPaint)
        canvas.drawText("TOTAL VALUE", 500f, 147f, textPaint)

        val products = listOf(
            arrayOf("SKU-9021", "UltraHD 4K Curved Monitor 32\"", "Displays", "45", "$280.00", "$12,600.00"),
            arrayOf("SKU-9022", "Mechanical Wireless Keyboard RGB", "Peripherals", "120", "$45.00", "$5,400.00"),
            arrayOf("SKU-9023", "Ergonomic Precision Mouse BT", "Peripherals", "180", "$28.50", "$5,130.00"),
            arrayOf("SKU-9024", "USB-C Dual 4K Docking Station", "Accessories", "90", "$65.00", "$5,850.00"),
            arrayOf("SKU-9025", "Noise Cancelling Studio Headset", "Audio", "60", "$110.00", "$6,600.00"),
            arrayOf("SKU-9026", "NVMe M.2 SSD PCIe 4.0 2TB", "Storage", "240", "$82.00", "$19,680.00"),
            arrayOf("SKU-9027", "100W GaN Fast Charger Multi-Port", "Power", "310", "$18.00", "$5,580.00"),
            arrayOf("SKU-9028", "Aluminium Adjustable Laptop Stand", "Accessories", "150", "$22.00", "$3,300.00")
        )

        var y = 180f
        paint.color = Color.parseColor("#E2E8F0")

        for (p in products) {
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 9.5f
            textPaint.isFakeBoldText = false
            canvas.drawText(p[0], 48f, y, textPaint)
            canvas.drawText(p[1], 130f, y, textPaint)
            canvas.drawText(p[2], 300f, y, textPaint)
            canvas.drawText(p[3], 395f, y, textPaint)
            canvas.drawText(p[4], 445f, y, textPaint)
            textPaint.isFakeBoldText = true
            canvas.drawText(p[5], 495f, y, textPaint)

            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)
            y += 28f
        }

        doc.finishPage(page)
    }

    private fun createSampleTimesheetPdf(doc: PdfDocument, paint: Paint, textPaint: Paint) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        paint.color = Color.parseColor("#4C1D95")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = true
        canvas.drawText("AGILE SPRINT TIMESHEET & BILLING", 40f, 45f, textPaint)

        textPaint.textSize = 11f
        textPaint.isFakeBoldText = false
        canvas.drawText("Period: Sprint 34  |  Project: Quantum Core Platform", 40f, 70f, textPaint)

        // Table Header
        paint.color = Color.parseColor("#2E1065")
        canvas.drawRect(40f, 130f, 555f, 155f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("DATE", 48f, 147f, textPaint)
        canvas.drawText("ENGINEER", 125f, 147f, textPaint)
        canvas.drawText("MODULE / TASK", 240f, 147f, textPaint)
        canvas.drawText("HOURS", 380f, 147f, textPaint)
        canvas.drawText("RATE ($)", 445f, 147f, textPaint)
        canvas.drawText("GROSS PAY", 500f, 147f, textPaint)

        val logs = listOf(
            arrayOf("2026-08-18", "Alex Rivera", "Auth & OAuth2 Flow Refactor", "8.0", "$85.00", "$680.00"),
            arrayOf("2026-08-19", "Elena Chen", "GraphQL Query Optimization", "7.5", "$90.00", "$675.00"),
            arrayOf("2026-08-20", "Marcus Vance", "K8s Helm Cluster Deployment", "8.5", "$95.00", "$807.50"),
            arrayOf("2026-08-21", "Sara Al-Mansoor", "Payment Webhooks Integration", "8.0", "$85.00", "$680.00"),
            arrayOf("2026-08-22", "Alex Rivera", "Database Migration & Indexes", "6.0", "$85.00", "$510.00"),
            arrayOf("2026-08-23", "Elena Chen", "CI/CD Pipeline Automation", "8.0", "$90.00", "$720.00"),
            arrayOf("2026-08-24", "Marcus Vance", "Load Testing & Redis Cache", "7.0", "$95.00", "$665.00")
        )

        var y = 180f
        paint.color = Color.parseColor("#E2E8F0")

        for (log in logs) {
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 9.5f
            textPaint.isFakeBoldText = false
            canvas.drawText(log[0], 48f, y, textPaint)
            canvas.drawText(log[1], 125f, y, textPaint)
            canvas.drawText(log[2], 240f, y, textPaint)
            canvas.drawText(log[3], 390f, y, textPaint)
            canvas.drawText(log[4], 450f, y, textPaint)
            textPaint.isFakeBoldText = true
            canvas.drawText(log[5], 500f, y, textPaint)

            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)
            y += 28f
        }

        doc.finishPage(page)
    }
}
