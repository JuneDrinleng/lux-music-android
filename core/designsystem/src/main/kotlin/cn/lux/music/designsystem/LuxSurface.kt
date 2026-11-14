package cn.lux.music.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Canvas from the final lux-earth-palette; components follow in UI stages. */
@Composable
public fun LuxSurface(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF3F0E8))) { content() }
}
