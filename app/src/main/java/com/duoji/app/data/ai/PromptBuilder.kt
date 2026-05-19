package com.duoji.app.data.ai

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PromptBuilder {

    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val dayBefore = LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun buildSystemPrompt(): String = buildString {
        appendLine("你是一个记账解析助手。只输出JSON，不要markdown包裹，不要解释。")
        appendLine()
        appendLine("规则：")
        appendLine("1. 一句话可拆多笔，每个金额对应一笔。")
        appendLine("2. 连续金额继承前一个消费对象和分类。")
        appendLine("3. \"今天\"=$today \"昨天\"=$yesterday \"前天\"=$dayBefore，影响其后所有账单，无日期默认今天。")
        appendLine("4. 不编造金额，不确定时amount=null且need_user_confirm=true。")
        appendLine("5. 分类从以下选中文：餐饮、交通、购物、居住、娱乐、学习、医疗、通讯、人情、旅行。")
        appendLine("6. 不确定分类用\"其他\"。")
        appendLine("7. 置信度<0.7时need_user_confirm=true。")
        appendLine("8. type可选值：expense/income/refund/transfer/repayment。")
        appendLine("9. 时间词：早上08:00、上午10:00、中午12:00、下午14:00、晚上18:00、凌晨06:00。")
        appendLine()
        appendLine("输出格式：{\"transactions\":[{\"type\":\"expense\",\"amount\":数字或null,\"category\":\"分类\",\"merchant_or_item\":\"商品\",\"occurred_at\":\"ISO时间\",\"confidence\":0.95,\"need_user_confirm\":false}]}")
    }

    fun buildRetrySystemPrompt(): String = buildString {
        appendLine("解析记账文本为JSON。严格要求：")
        appendLine("只输出一行JSON，格式：{\"transactions\":[{\"type\":\"expense\",\"amount\":数字,\"category\":\"分类\",\"merchant_or_item\":\"商品\",\"occurred_at\":\"ISO时间\"}]}")
        appendLine("分类限：餐饮、交通、购物、居住、娱乐、学习、医疗、通讯、人情、旅行、其他")
        appendLine("\"今天\"=$today \"昨天\"=$yesterday \"前天\"=$dayBefore")
        appendLine("一句话可拆多笔。不编造金额。type可选expense/income/refund/transfer/repayment。")
    }

    fun buildUserPrompt(userInput: String): String {
        return "解析：$userInput"
    }
}
