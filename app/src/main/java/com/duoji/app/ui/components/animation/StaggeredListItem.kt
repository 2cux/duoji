package com.duoji.app.ui.components.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun StaggeredListItem(
    index: Int,
    delayPerItem: Int = 50,
    animDuration: Int = 350,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { 20.dp.toPx() }

    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            show = true
        }
    }

    AnimatedVisibility(
        visible = show && visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = animDuration,
                delayMillis = index * delayPerItem
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = animDuration,
                delayMillis = index * delayPerItem
            )
        ) { offsetPx.toInt() },
        modifier = modifier
    ) {
        content()
    }
}
