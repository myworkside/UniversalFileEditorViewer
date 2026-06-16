package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.util.Log
import com.sumitupdat.universalfileeditorviewer.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

private const val TAG = "SpreadsheetRepository"

class SpreadsheetRepository(private val context: Context) {

    suspend fun readSpreadsheet(file: File): SpreadsheetData = withContext(Dispatchers.IO) {
        val sheets = mutableListOf<SheetData>()
        try {
            FileInputStream(file).use { fis ->
                val workbook: Workbook = if (file.extension.lowercase() == "xlsx") {
                    XSSFWorkbook(fis)
                } else {
                    HSSFWorkbook(fis)
                }

                for (i in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(i)
                    val rows = mutableListOf<RowData>()
                    var maxCols = 0
                    
                    // Limit rows for performance if needed, or implement paging later
                    val rowLimit = 1000 
                    val lastRowNum = sheet.lastRowNum.coerceAtMost(rowLimit)
                    
                    for (rowIndex in 0..lastRowNum) {
                        val row = sheet.getRow(rowIndex)
                        if (row == null) {
                            rows.add(RowData(emptyList()))
                            continue
                        }
                        
                        val cells = mutableListOf<CellData>()
                        val lastCellNum = row.lastCellNum.toInt()
                        if (lastCellNum > maxCols) maxCols = lastCellNum
                        
                        for (cellIndex in 0 until lastCellNum) {
                            val cell = row.getCell(cellIndex)
                            cells.add(parseCell(cell))
                        }
                        rows.add(RowData(cells))
                    }
                    sheets.add(SheetData(sheet.sheetName, rows, maxCols))
                }
                workbook.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading spreadsheet: ${file.name}", e)
        }
        SpreadsheetData(sheets)
    }

    private fun parseCell(cell: Cell?): CellData {
        if (cell == null) return CellData("")
        
        val value = try {
            when (cell.cellType) {
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        cell.dateCellValue.toString()
                    } else {
                        cell.numericCellValue.toString().removeSuffix(".0")
                    }
                }
                CellType.STRING -> cell.stringCellValue
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.richStringCellValue.string
                    } catch (e: Exception) {
                        try {
                            cell.numericCellValue.toString()
                        } catch (e2: Exception) {
                            cell.cellFormula
                        }
                    }
                }
                CellType.BLANK -> ""
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }

        val style = cell.cellStyle
        val font = cell.sheet.workbook.getFontAt(style.fontIndex)
        
        return CellData(
            value = value,
            isBold = font.bold,
            isHeader = cell.rowIndex == 0 // Simple heuristic
        )
    }
}
