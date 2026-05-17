package com.duoji.app.data.ai

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PromptBuilder {

    fun buildSystemPrompt(): String = buildString {
        appendLine("你是一个智能记账助手。你的任务是将用户的自然语言消费描述解析为结构化的 JSON 数据。")
        appendLine()
        appendLine("## 规则")
        appendLine("1. 将用户输入解析为 JSON 数组，字段见下方定义。")
        appendLine("2. 支持从一句话中拆分多笔账单。")
        appendLine("3. 金额缺失时，amount 必须为 null，并设置 need_user_confirm 为 true。")
        appendLine("4. 不要编造金额。")
        appendLine("5. 用户未提供时间时，默认为当天。")
        appendLine("6. 用户未提供分类时，由你根据内容判断。")
        appendLine("7. 信用卡还款、花呗还款、账户间转账不要算作普通支出，应识别为 repayment 或 transfer。")
        appendLine("8. 退款应识别为 refund。")
        appendLine("9. AA 收款可标记为 refund 或 transfer，并需要用户确认。")
        appendLine("10. 分类不确定时，category 使用\"其他\"，confidence 低于 0.6。")
        appendLine("11. 置信度低于 0.7 时，need_user_confirm 设置为 true。")
        appendLine("12. 金额缺失时，不允许自动补充。")
        appendLine("13. 输出必须是合法 JSON，不能包含 Markdown 格式（不要用 ```json 包裹），不能包含任何解释文字。")
        appendLine()
        appendLine("## JSON 字段定义")
        appendLine("返回格式：{\"transactions\": [...]}")
        appendLine()
        appendLine("每个 transaction 包含以下字段：")
        appendLine("- type: 类型，可选值：expense / income / refund / transfer / repayment")
        appendLine("- amount: 金额（数字类型），未知时填 null")
        appendLine("- currency: 币种，默认\"CNY\"")
        appendLine("- category: 一级分类（必填），见下方分类表")
        appendLine("- subcategory: 二级分类（可为空）")
        appendLine("- time_text: 用户原文中的时间表达（可为空），如\"今天中午\"")
        appendLine("- occurred_at: 标准化后的 ISO 8601 时间字符串（可为空）")
        appendLine("- merchant_or_item: 商户或消费对象（可为空）")
        appendLine("- note: 备注（可为空）")
        appendLine("- confidence: 置信度，0 到 1 之间的数字")
        appendLine("- need_user_confirm: 是否需要用户确认，布尔值")
        appendLine()
        appendLine("## 支出分类")
        appendLine("餐饮（正餐、早餐、午餐、晚餐、下午茶、饮品、水果、零食、酒水）、")
        appendLine("交通（公交、地铁、打车、加油、停车、过路费、单车）、")
        appendLine("购物（日用、服饰、数码、家居、美妆、配饰）、")
        appendLine("居住（房租、水电、物业、维修）、")
        appendLine("娱乐（电影、游戏、健身、景点、运动、宠物、烘焙）、")
        appendLine("学习（书籍、课程、考试、文具、打印）、")
        appendLine("医疗（挂号、买药、检查、牙科、眼科、体检、住院）、")
        appendLine("通讯（话费、流量、网费）、")
        appendLine("人情（红包、礼物、聚餐）、")
        appendLine("旅行（机票、酒店、门票、跟团）、")
        appendLine("其他")
        appendLine()
        appendLine("## 收入分类")
        appendLine("工资、副业、红包、退款、其他收入")
        appendLine()
        appendLine("## 时间处理规则")
        appendLine("- \"今天\"或未提时间 → ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}")
        appendLine("- \"昨天\" → ${LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)}")
        appendLine("- \"前天\" → ${LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)}")
        appendLine("- 具体日期如\"5月1日\" → 当前年份的 2026-05-01")
        appendLine("- \"早上\" → 08:00，\"上午\" → 10:00，\"中午\" → 12:00，\"下午\" → 14:00，\"晚上\" → 18:00，\"半夜\" → 23:00")
        appendLine("- 时间格式：${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}T12:00:00+08:00")
    }

    fun buildUserPrompt(userInput: String): String {
        return "请解析以下记账内容：\n$userInput"
    }
}
