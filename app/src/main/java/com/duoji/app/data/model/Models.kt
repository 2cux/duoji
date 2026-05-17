package com.duoji.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TransactionType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    REFUND("退款"),
    TRANSFER("转账"),
    REPAYMENT("还款");

    companion object {
        fun fromLabel(label: String): TransactionType {
            return entries.find { it.label == label } ?: EXPENSE
        }
    }
}

enum class ExpenseCategory(val label: String) {
    FOOD("餐饮"),
    TRANSPORT("交通"),
    SHOPPING("购物"),
    HOUSING("居住"),
    ENTERTAINMENT("娱乐"),
    EDUCATION("学习"),
    MEDICAL("医疗"),
    COMMUNICATION("通讯"),
    SOCIAL("人情"),
    TRAVEL("旅行"),
    OTHER("其他");

    companion object {
        fun fromLabel(label: String): ExpenseCategory {
            return entries.find { it.label == label } ?: OTHER
        }
    }
}

enum class IncomeCategory(val label: String) {
    SALARY("工资"),
    SIDE("副业"),
    RED_PACKET("红包"),
    REFUND("退款"),
    OTHER("其他收入");

    companion object {
        fun fromLabel(label: String): IncomeCategory {
            return entries.find { it.label == label } ?: OTHER
        }
    }
}

@Serializable
data class AIParseRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<AIMessage> = emptyList(),
    @SerialName("temperature")
    val temperature: Double = 0.1
)

@Serializable
data class AIMessage(
    val role: String,
    val content: String
)

@Serializable
data class AIResponse(
    val choices: List<AIChoice> = emptyList(),
    val error: AIError? = null
)

@Serializable
data class AIChoice(
    val message: AIResponseMessage
)

@Serializable
data class AIResponseMessage(
    val content: String
)

@Serializable
data class AIError(
    val message: String = ""
)

@Serializable
data class AITransaction(
    val type: String = "expense",
    val amount: Double? = null,
    val currency: String = "CNY",
    val category: String = "其他",
    val subcategory: String? = null,
    @SerialName("time_text")
    val timeText: String? = null,
    @SerialName("occurred_at")
    val occurredAt: String? = null,
    @SerialName("merchant_or_item")
    val merchantOrItem: String? = null,
    val note: String? = null,
    val confidence: Double = 1.0,
    @SerialName("need_user_confirm")
    val needUserConfirm: Boolean = false
)

@Serializable
data class AIParseResult(
    val transactions: List<AITransaction> = emptyList()
)

data class TransactionDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double? = null,
    val amountText: String = "",
    val currency: String = "CNY",
    val category: String = "",
    val subcategory: String = "",
    val timeText: String = "",
    val occurredAt: String = "",
    val merchantOrItem: String = "",
    val note: String = "",
    val confidence: Double = 1.0,
    val needUserConfirm: Boolean = false
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val category: String,
    val subcategory: String,
    val note: String,
    val merchantOrItem: String,
    val occurredAt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
