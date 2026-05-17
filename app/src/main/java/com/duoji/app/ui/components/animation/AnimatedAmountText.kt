package com.duoji.app.ui.components.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AnimatedAmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "¥ ",
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textAlign: TextAlign = TextAlign.Start
) {
    val displayText = formatAnimAmount(amount)

    AnimatedContent(
        targetState = displayText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300)) +
                    androidx.compose.animation.slideInVertically(
                        animationSpec = tween(300)
                    ) { it / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(200)) +
                            androidx.compose.animation.slideOutVertically(
                                animationSpec = tween(200)
                            ) { -it / 4 }
                )
        },
        label = "amountAnim",
        modifier = modifier
    ) { text ->
        Text(
            text = "$prefix$text",
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign
        )
    }
}

private fun formatAnimAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}
