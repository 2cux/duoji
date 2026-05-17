package com.duoji.app.ui.components.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@Composable
fun AnimatedSection(
    visible: Boolean = true,
    delayMillis: Int = 0,
    animDuration: Int = 400,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { 24.dp.toPx() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = animDuration, delayMillis = delayMillis)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = animDuration, delayMillis = delayMillis)
        ) { offsetPx },
        modifier = modifier
    ) {
        content()
    }
}
