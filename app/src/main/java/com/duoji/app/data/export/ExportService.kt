package com.duoji.app.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.duoji.app.data.local.entity.TransactionEntity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportService(private val context: Context) {

    private fun getExportDir(): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportCsv(transactions: List<TransactionEntity>): Result<String> {
        return try {
            val content = CsvExportBuilder.build(transactions)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val file = File(getExportDir(), "duoji_transactions_${timestamp}.csv")
            file.writeText(content, Charsets.UTF_8)
            shareFile(file, "text/csv")
            Result.success("CSV 已生成，正在打开分享…")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportJson(transactions: List<TransactionEntity>): Result<String> {
        return try {
            val content = JsonExportBuilder.build(transactions)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val file = File(getExportDir(), "duoji_transactions_${timestamp}.json")
            file.writeText(content, Charsets.UTF_8)
            shareFile(file, "application/json")
            Result.success("JSON 已生成，正在打开分享…")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "导出账单")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
