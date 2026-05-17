package com.duoji.app.ui.theme

import androidx.compose.ui.graphics.Color

val WarmBackground = Color(0xFFFFF7EF)
val WarmPrimary = Color(0xFFFF9F5A)
val WarmPrimaryVariant = Color(0xFFFFB178)
val WarmAccent = Color(0xFFFF7A59)
val WarmSecondary = Color(0xFFFFD8A8)
val WarmTextPrimary = Color(0xFF3A2A22)
val WarmTextSecondary = Color(0xFF8A6F61)
val WarmCard = Color(0xFFFFFFFF)
val WarmCardAlt = Color(0xFFFFFDF9)
val WarmIncome = Color(0xFF7BCFA6)
val IncomeLight = Color(0xFFE8F8F0)
val WarmExpense = Color(0xFFFF9F5A)
val ExpenseLight = Color(0xFFFFF0E4)
val WarmWarning = Color(0xFFF6B35B)
val WarningLight = Color(0xFFFEF4E5)
val WarmSurface = Color(0xFFFFF7EF)
val WarmOnPrimary = Color(0xFFFFFFFF)

// Gradient colors for hero card
val GradientStart = Color(0xFFFFE0B8)
val GradientMid = Color(0xFFFFB178)
val GradientEnd = Color(0xFFFF8C66)

// Category colors
val CategoryFood = Color(0xFFFF9F5A)
val CategoryTransport = Color(0xFF7EC8E3)
val CategoryShopping = Color(0xFFF6A5C0)
val CategoryHousing = Color(0xFFA8D8A8)
val CategoryEntertainment = Color(0xFFD4A5F6)
val CategoryEducation = Color(0xFF7BCFA6)
val CategoryMedical = Color(0xFFF6B35B)
val CategoryCommunication = Color(0xFF82B3E0)
val CategorySocial = Color(0xFFFF8C8C)
val CategoryTravel = Color(0xFF82D4D4)
val CategoryOther = Color(0xFFB8A8A0)

fun categoryColor(category: String): Color = when (category) {
    "餐饮" -> CategoryFood
    "交通" -> CategoryTransport
    "购物" -> CategoryShopping
    "居住" -> CategoryHousing
    "娱乐" -> CategoryEntertainment
    "学习" -> CategoryEducation
    "医疗" -> CategoryMedical
    "通讯" -> CategoryCommunication
    "人情" -> CategorySocial
    "旅行" -> CategoryTravel
    else -> CategoryOther
}
