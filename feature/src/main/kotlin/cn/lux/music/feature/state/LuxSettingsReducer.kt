package cn.lux.music.feature.state

internal object LuxSettingsReducer {
    private val OptionTitles: Map<String, String> =
        mapOf("lang" to "语言", "theme" to "主题", "src" to "搜索来源", "statsStyle" to "统计页样式")

    private val SourceNameToCode: Map<String, String> =
        mapOf("酷我" to "kw", "网易" to "wy", "QQ" to "qq")

    fun reduce(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? = reduceSettings(state, event) ?: reduceProfileAndStats(state, event)

    private fun reduceSettings(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.UpdateSettingsQuery -> state.copy(settingsQuery = event.query)
            is LuxUiEvent.ToggleSwitch -> toggleSwitch(state, event.id)
            is LuxUiEvent.PickOption -> pickOption(state, event.optionId, event.value)
            is LuxUiEvent.SetRoomLayout -> state.copy(roomLayout = event.layout)
            is LuxUiEvent.SetVersionModal -> state.copy(versionModalOpen = event.open)
            else -> null
        }

    private fun reduceProfileAndStats(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.SetNick -> state.copy(nick = event.nick).withToast("昵称已改为 ${event.nick}（仅预览）")
            is LuxUiEvent.SetAccountMode -> state.copy(accountMode = event.mode)
            LuxUiEvent.DemoLogin -> state.copy(syncConnected = true).withToast("已切换为示例「已连接」状态")
            is LuxUiEvent.SetStatsPeriod -> state.copy(statsPeriod = event.period)
            is LuxUiEvent.SetStatsChartKind -> state.copy(statsChartKind = event.kind)
            is LuxUiEvent.ShowToast -> state.withToast(event.message)
            LuxUiEvent.ClearToast -> state.copy(toastMessage = null)
            else -> null
        }

    private fun toggleSwitch(
        state: LuxUiState,
        id: String,
    ): LuxUiState =
        when (id) {
            "tr" -> state.copy(swDefaultTr = !state.swDefaultTr, showTranslation = !state.swDefaultTr)
            "gapless" -> state.copy(swGapless = !state.swGapless)
            "hq" -> state.copy(swHq = !state.swHq)
            "cellular" -> state.copy(swCellular = !state.swCellular)
            else -> state
        }

    private fun pickOption(
        state: LuxUiState,
        optionId: String,
        value: String,
    ): LuxUiState {
        val label = OptionTitles[optionId] ?: "选项"
        val updated =
            when (optionId) {
                "lang" -> state.copy(lang = value)
                "theme" -> state.copy(theme = value)
                "src" -> state.copy(defaultSearchSource = value, searchSource = SourceNameToCode[value] ?: "all")
                "statsStyle" -> state.copy(statsStyle = value)
                else -> state
            }
        return updated.copy(stack = state.stack.dropLast(1)).withToast("$label：$value")
    }
}
