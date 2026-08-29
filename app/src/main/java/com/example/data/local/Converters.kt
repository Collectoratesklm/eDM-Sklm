package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ColumnType
import com.example.data.model.SpreadsheetSheet
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromSheetsList(sheets: List<SpreadsheetSheet>): String {
        return sheetsToJson(sheets)
    }

    @TypeConverter
    fun toSheetsList(jsonStr: String): List<SpreadsheetSheet> {
        return sheetsFromJson(jsonStr)
    }

    companion object {
        fun sheetsToJson(sheets: List<SpreadsheetSheet>): String {
            val rootArray = JSONArray()
            for (sheet in sheets) {
                val sheetObj = JSONObject()
                sheetObj.put("name", sheet.name)

                val headersArr = JSONArray()
                sheet.headers.forEach { headersArr.put(it) }
                sheetObj.put("headers", headersArr)

                val typesArr = JSONArray()
                sheet.columnTypes.forEach { typesArr.put(it.name) }
                sheetObj.put("columnTypes", typesArr)

                val rowsArr = JSONArray()
                for (row in sheet.rows) {
                    val rowArr = JSONArray()
                    row.forEach { rowArr.put(it) }
                    rowsArr.put(rowArr)
                }
                sheetObj.put("rows", rowsArr)

                val summaryObj = JSONObject()
                sheet.summaryInfo.forEach { (k, v) -> summaryObj.put(k, v) }
                sheetObj.put("summaryInfo", summaryObj)

                rootArray.put(sheetObj)
            }
            return rootArray.toString()
        }

        fun sheetsFromJson(jsonStr: String): List<SpreadsheetSheet> {
            if (jsonStr.isBlank()) return emptyList()
            val list = mutableListOf<SpreadsheetSheet>()
            try {
                val rootArray = JSONArray(jsonStr)
                for (i in 0 until rootArray.length()) {
                    val sheetObj = rootArray.getJSONObject(i)
                    val name = sheetObj.optString("name", "Sheet${i + 1}")

                    val headersList = mutableListOf<String>()
                    val headersArr = sheetObj.optJSONArray("headers")
                    if (headersArr != null) {
                        for (h in 0 until headersArr.length()) {
                            headersList.add(headersArr.getString(h))
                        }
                    }

                    val typesList = mutableListOf<ColumnType>()
                    val typesArr = sheetObj.optJSONArray("columnTypes")
                    if (typesArr != null) {
                        for (t in 0 until typesArr.length()) {
                            val typeName = typesArr.optString(t, "TEXT")
                            val cType = try {
                                ColumnType.valueOf(typeName)
                            } catch (e: Exception) {
                                ColumnType.TEXT
                            }
                            typesList.add(cType)
                        }
                    }

                    val rowsList = mutableListOf<List<String>>()
                    val rowsArr = sheetObj.optJSONArray("rows")
                    if (rowsArr != null) {
                        for (r in 0 until rowsArr.length()) {
                            val rowArr = rowsArr.getJSONArray(r)
                            val rowCells = mutableListOf<String>()
                            for (c in 0 until rowArr.length()) {
                                rowCells.add(rowArr.optString(c, ""))
                            }
                            rowsList.add(rowCells)
                        }
                    }

                    val summaryMap = mutableMapOf<String, String>()
                    val summaryObj = sheetObj.optJSONObject("summaryInfo")
                    if (summaryObj != null) {
                        val keys = summaryObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            summaryMap[k] = summaryObj.optString(k, "")
                        }
                    }

                    val sheet = SpreadsheetSheet(
                        name = name,
                        headers = headersList,
                        rows = rowsList,
                        columnTypes = typesList,
                        summaryInfo = summaryMap
                    ).withDetectedColumnTypes()

                    list.add(sheet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}
