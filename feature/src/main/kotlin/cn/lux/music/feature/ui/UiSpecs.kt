package cn.lux.music.feature.ui

import cn.lux.music.designsystem.LuxButtonStyle

internal data class SimpleSubSpec(
    val title: String,
    val bodyText: String,
    val buttonText: String? = null,
    val buttonStyle: LuxButtonStyle = LuxButtonStyle.Primary,
    val toastMessage: String = "",
)

internal data class StatsDataset(
    val labels: List<String>,
    val values: List<Int>,
    val total: String,
    val unit: String,
    val date: String,
    val subtitle: String,
)
