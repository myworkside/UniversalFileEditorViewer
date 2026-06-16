package com.sumitupdat.universalfileeditorviewer.data.model

data class SpreadsheetData(
    val sheets: List<SheetData>
)

data class SheetData(
    val name: String,
    val rows: List<RowData>,
    val maxColumns: Int
)

data class RowData(
    val cells: List<CellData>
)

data class CellData(
    val value: String,
    val isHeader: Boolean = false,
    val backgroundColor: Int? = null,
    val textColor: Int? = null,
    val isBold: Boolean = false
)
