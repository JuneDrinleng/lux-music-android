package cn.lux.music.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

private val CoverResourceMap: Map<String, Int> =
    mapOf(
        "c1" to R.drawable.cover_c1,
        "c2" to R.drawable.cover_c2,
        "c3" to R.drawable.cover_c3,
        "c4" to R.drawable.cover_c4,
        "c5" to R.drawable.cover_c5,
        "c6" to R.drawable.cover_c6,
        "c7" to R.drawable.cover_c7,
        "c8" to R.drawable.cover_c8,
        "c9" to R.drawable.cover_c9,
        "c10" to R.drawable.cover_c10,
        "c11" to R.drawable.cover_c11,
        "c12" to R.drawable.cover_c12,
        "c13" to R.drawable.cover_c13,
        "c14" to R.drawable.cover_c14,
    )

/** Maps the 14 deterministic cover keys (`c1`..`c14`) from `ui/index.html` to bundled assets. */
@DrawableRes
public fun luxCoverRes(key: String): Int = CoverResourceMap[key] ?: R.drawable.cover_c1

/** Borderless cover with the warm lifted shadow from `lux-seam-and-cover-polish`. */
@Composable
public fun LuxCover(
    coverKey: String,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    cornerRadius: Dp = LuxDimens.RadiusSm,
) {
    val sized = if (size != null) modifier.size(size) else modifier
    Box(
        modifier =
            sized
                .luxCoverShadow(cornerRadius)
                .clip(RoundedCornerShape(cornerRadius))
                .background(LuxColors.Page),
    ) {
        Image(
            painter = painterResource(luxCoverRes(coverKey)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 2x2 collage cover matching `.cover.collage` in `ui/index.html`. */
@Composable
public fun LuxCollage(
    coverKeys: List<String>,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LuxDimens.RadiusSm,
) {
    val padded =
        List(4) { index ->
            coverKeys.getOrElse(index) { coverKeys.firstOrNull() ?: "c1" }
        }
    Box(
        modifier =
            modifier
                .size(size)
                .luxCoverShadow(cornerRadius)
                .clip(RoundedCornerShape(cornerRadius))
                .background(LuxColors.Page),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                CollageTile(padded[0], Modifier.weight(1f).fillMaxHeight())
                CollageTile(padded[1], Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                CollageTile(padded[2], Modifier.weight(1f).fillMaxHeight())
                CollageTile(padded[3], Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CollageTile(
    coverKey: String,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(luxCoverRes(coverKey)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
