package com.duoji.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val subcategory: String?,
    val note: String,
    val merchantOrItem: String?,
    val occurredAt: Long,
    val occurredAtText: String?,
    val source: String,
    val rawText: String?,
    val confidence: Double,
    val needUserConfirm: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
