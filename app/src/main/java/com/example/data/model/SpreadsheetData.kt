package com.example.data.model

enum class ExtractionPreset(
    val id: String,
    val displayName: String,
    val iconName: String,
    val description: String,
    val defaultPrompt: String,
    val suggestedColumns: List<String>
) {
    AUTO_DETECT(
        id = "auto_detect",
        displayName = "Auto-Detect Tables",
        iconName = "TableChart",
        description = "Intelligently extracts all tabular structures, matrixes, and key figures.",
        defaultPrompt = "Extract all tables and structured data found in this PDF document accurately. Preserve column titles, row values, sub-totals, and header hierarchy.",
        suggestedColumns = emptyList()
    ),
    INVOICE_RECEIPT(
        id = "invoice_receipt",
        displayName = "Invoices & Receipts",
        iconName = "ReceiptLong",
        description = "Extracts vendor, invoice number, line items, unit prices, taxes, and totals.",
        defaultPrompt = "Extract invoice and receipt details into structured tables. Extract vendor metadata as a summary table, and itemized rows (Item Description, Quantity, Unit Price, Tax, Total Amount) as the primary data table.",
        suggestedColumns = listOf("Item / Description", "Qty", "Unit Price", "Discount", "Tax", "Amount")
    ),
    FINANCIAL_STATEMENT(
        id = "financial_statement",
        displayName = "Bank & Financials",
        iconName = "AccountBalance",
        description = "Extracts transaction ledgers, dates, debit/credit entries, and running balances.",
        defaultPrompt = "Extract financial statements, bank accounts, or ledger records into rows with Date, Transaction Description, Reference/Check No, Debit/Withdrawal, Credit/Deposit, and Account Balance.",
        suggestedColumns = listOf("Date", "Description", "Ref No", "Debit (-)", "Credit (+)", "Balance")
    ),
    INVENTORY_CATALOG(
        id = "inventory_catalog",
        displayName = "Inventory & Catalog",
        iconName = "Inventory2",
        description = "Extracts item SKUs, descriptions, categories, stock counts, and unit costs.",
        defaultPrompt = "Extract inventory and product lists into columns: SKU/Item Code, Product Name, Category, In Stock Qty, Unit Cost, Retail Price, and Total Value.",
        suggestedColumns = listOf("SKU / Code", "Product Name", "Category", "Stock Qty", "Unit Cost", "Total Value")
    ),
    TIMESHEET_PAYROLL(
        id = "timesheet_payroll",
        displayName = "Timesheets & Hours",
        iconName = "Schedule",
        description = "Extracts employee logs, dates, hours worked, hourly rates, and pay amounts.",
        defaultPrompt = "Extract employee timesheet or payroll records into columns: Date, Employee Name, Department/Task, Regular Hours, Overtime, Rate ($), Gross Pay ($).",
        suggestedColumns = listOf("Date", "Employee Name", "Task/Project", "Hours", "Rate", "Total Pay")
    ),
    CUSTOM_SCHEMA(
        id = "custom_schema",
        displayName = "Custom Mapping",
        iconName = "Tune",
        description = "Map specific target columns and write custom AI extraction rules.",
        defaultPrompt = "Extract data mapped to the user-specified custom columns below.",
        suggestedColumns = emptyList()
    )
}

enum class ColumnType(val label: String) {
    TEXT("Text"),
    NUMBER("Number"),
    CURRENCY("Currency"),
    DATE("Date"),
    PERCENTAGE("Percentage"),
    BOOLEAN("Boolean");

    companion object {
        fun detectType(values: List<String>): ColumnType {
            if (values.isEmpty()) return TEXT
            val nonBlank = values.filter { it.isNotBlank() }
            if (nonBlank.isEmpty()) return TEXT

            var currencyCount = 0
            var numberCount = 0
            var percentCount = 0
            var dateCount = 0
            var boolCount = 0

            for (v in nonBlank) {
                val trimmed = v.trim()
                if (trimmed.startsWith("$") || trimmed.startsWith("€") || trimmed.startsWith("£") || trimmed.startsWith("¥") || trimmed.endsWith("USD") || trimmed.endsWith("EUR")) {
                    currencyCount++
                } else if (trimmed.endsWith("%")) {
                    percentCount++
                } else if (trimmed.equals("true", ignoreCase = true) || trimmed.equals("false", ignoreCase = true) || trimmed.equals("yes", ignoreCase = true) || trimmed.equals("no", ignoreCase = true)) {
                    boolCount++
                } else if (trimmed.matches(Regex("^\\d{1,4}[-/.]\\d{1,2}[-/.]\\d{1,4}$")) || trimmed.matches(Regex("^[A-Za-z]{3,9}\\s+\\d{1,2},\\s*\\d{4}$"))) {
                    dateCount++
                } else {
                    val cleanNum = trimmed.replace(",", "").replace("$", "").replace("€", "")
                    if (cleanNum.toDoubleOrNull() != null) {
                        numberCount++
                    }
                }
            }

            val threshold = (nonBlank.size * 0.6)
            return when {
                currencyCount >= threshold -> CURRENCY
                percentCount >= threshold -> PERCENTAGE
                boolCount >= threshold -> BOOLEAN
                dateCount >= threshold -> DATE
                (numberCount + currencyCount) >= threshold -> NUMBER
                else -> TEXT
            }
        }
    }
}

data class SpreadsheetSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val columnTypes: List<ColumnType> = emptyList(),
    val summaryInfo: Map<String, String> = emptyMap()
) {
    fun withDetectedColumnTypes(): SpreadsheetSheet {
        if (columnTypes.isNotEmpty() && columnTypes.size == headers.size) return this
        val types = headers.indices.map { colIndex ->
            val colValues = rows.mapNotNull { row -> row.getOrNull(colIndex) }
            ColumnType.detectType(colValues)
        }
        return copy(columnTypes = types)
    }

    val rowCount: Int get() = rows.size
    val columnCount: Int get() = headers.size
}

data class SpreadsheetDocument(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val sourcePages: Int = 1,
    val sheets: List<SpreadsheetSheet>,
    val createdAt: Long = System.currentTimeMillis(),
    val presetUsed: ExtractionPreset = ExtractionPreset.AUTO_DETECT,
    val notes: String = "",
    val confidenceScore: Float = 0.95f,
    val isFavorite: Boolean = false
) {
    val totalRows: Int get() = sheets.sumOf { it.rowCount }
    val totalSheets: Int get() = sheets.size
}

data class SamplePdfInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val pageCount: Int,
    val defaultPreset: ExtractionPreset,
    val estimatedRows: Int
)

data class ExtractionProgress(
    val currentStep: String,
    val detailMessage: String,
    val progressFraction: Float, // 0.0f to 1.0f
    val isDone: Boolean = false,
    val error: String? = null
)
