package com.duoji.app.data.model

fun AITransaction.toTransactionDraft(): TransactionDraft {
    val type = when (this.type) {
        "income" -> TransactionType.INCOME
        "refund" -> TransactionType.REFUND
        "transfer" -> TransactionType.TRANSFER
        "repayment" -> TransactionType.REPAYMENT
        else -> TransactionType.EXPENSE
    }

    return TransactionDraft(
        type = type,
        amount = this.amount,
        amountText = this.amount?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "",
        currency = this.currency,
        category = this.category,
        subcategory = this.subcategory ?: "",
        timeText = this.timeText ?: "",
        occurredAt = this.occurredAt ?: "",
        merchantOrItem = this.merchantOrItem ?: "",
        note = this.note ?: "",
        confidence = this.confidence,
        needUserConfirm = this.needUserConfirm
    )
}
