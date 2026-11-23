package cn.lux.music.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Tap-to-seek progress slider matching `.seek` in `ui/index.html`. */
@Composable
public fun LuxSeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(onSeek) {
                    detectTapGestures { offset ->
                        if (size.width > 0) {
                            onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                },
    ) {
        val trackShape = RoundedCornerShape(2.dp)
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(trackShape)
                    .background(LuxColors.Page)
                    .luxInnerShadow(blurRadius = 3.dp, offset = 1.dp, cornerRadius = 2.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(clamped)
                        .fillMaxHeight()
                        .background(LuxBrushes.SeekFill),
            )
        }
        val thumbOffset = (maxWidth * clamped) - 7.dp
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset.coerceIn(0.dp, (maxWidth - 14.dp).coerceAtLeast(0.dp)))
                    .size(14.dp)
                    .luxNeuUpSmall(7.dp)
                    .clip(CircleShape)
                    .background(LuxBrushes.RaisedFill)
                    .border(1.dp, Color.White, CircleShape),
        )
    }
}
