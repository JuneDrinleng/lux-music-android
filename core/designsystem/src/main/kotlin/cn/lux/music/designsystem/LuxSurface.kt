package cn.lux.music.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Root warm-linen canvas with the seamless translucent top status fade from `lux-seamless-status-frost-v3`. */
@Composable
public fun LuxSurface(content: @Composable BoxScope.() -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(LuxColors.Page)) {
        content()
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBarHeight + 12.dp)
                    .background(LuxBrushes.StatusFrostFade),
        )
    }
}
