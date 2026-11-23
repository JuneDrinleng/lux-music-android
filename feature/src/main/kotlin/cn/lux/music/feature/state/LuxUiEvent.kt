package cn.lux.music.feature.state

internal sealed interface LuxUiEvent {
    data class SelectTab(
        val tab: LuxTab,
    ) : LuxUiEvent

    data object GoDiscover : LuxUiEvent

    data object GoRadio : LuxUiEvent

    data object StartRadio : LuxUiEvent

    data class PushPage(
        val page: LuxSubPage,
    ) : LuxUiEvent

    data object PopPage : LuxUiEvent

    data object SystemBack : LuxUiEvent

    data object OpenPlayer : LuxUiEvent

    data object ClosePlayer : LuxUiEvent

    data class SetPlayerView(
        val view: LuxPlayerView,
    ) : LuxUiEvent

    data object OpenQueue : LuxUiEvent

    data object CloseQueue : LuxUiEvent

    data object ClearQueue : LuxUiEvent

    data class PlayQueueIndex(
        val index: Int,
    ) : LuxUiEvent

    data class PlayFromList(
        val listKey: String,
        val index: Int,
    ) : LuxUiEvent

    data class PlayAll(
        val listKey: String,
    ) : LuxUiEvent

    data class ShuffleAll(
        val listKey: String,
    ) : LuxUiEvent

    data object TogglePlay : LuxUiEvent

    data object NextTrack : LuxUiEvent

    data object PrevTrack : LuxUiEvent

    data object CyclePlayMode : LuxUiEvent

    data class SeekFraction(
        val fraction: Float,
    ) : LuxUiEvent

    data class SeekLyricLine(
        val index: Int,
    ) : LuxUiEvent

    data class Tick(
        val deltaSec: Double,
    ) : LuxUiEvent

    data class ToggleLike(
        val songId: String? = null,
    ) : LuxUiEvent

    data object ToggleTranslation : LuxUiEvent

    data object ToggleRomanization : LuxUiEvent

    data class ToggleCommentLike(
        val key: String,
    ) : LuxUiEvent

    data class SetCommentSort(
        val sort: String,
    ) : LuxUiEvent

    data object ToggleMoreSheet : LuxUiEvent

    data class SetSleepTimer(
        val timer: String,
    ) : LuxUiEvent

    data class UpdateSearchQuery(
        val query: String,
    ) : LuxUiEvent

    data object ClearSearch : LuxUiEvent

    data class PickSearchWord(
        val word: String,
    ) : LuxUiEvent

    data class DeleteSearchWord(
        val word: String,
    ) : LuxUiEvent

    data object ClearSearchHistory : LuxUiEvent

    data class SetSearchTab(
        val tab: String,
    ) : LuxUiEvent

    data object ToggleSearchSourceMenu : LuxUiEvent

    data class SetSearchSource(
        val source: String,
    ) : LuxUiEvent

    data class SetPlaylistView(
        val view: String,
    ) : LuxUiEvent

    data object TogglePlaylistSortMenu : LuxUiEvent

    data class PickPlaylistSort(
        val sort: String,
    ) : LuxUiEvent

    data class SetPlaylistMode(
        val mode: String,
    ) : LuxUiEvent

    data class UpdatePlaylistNameDraft(
        val name: String,
    ) : LuxUiEvent

    data class SetSmartRule(
        val rule: String,
    ) : LuxUiEvent

    data class SetSmartName(
        val name: String,
    ) : LuxUiEvent

    data object CreatePlaylist : LuxUiEvent

    data object CreateSmartPlaylist : LuxUiEvent

    data class RenamePlaylist(
        val playlistId: String,
    ) : LuxUiEvent

    data class DeletePlaylist(
        val playlistId: String,
    ) : LuxUiEvent

    data class AddSongToPlaylist(
        val songId: String,
        val playlistId: String,
    ) : LuxUiEvent

    data class UpdateSettingsQuery(
        val query: String,
    ) : LuxUiEvent

    data class ToggleSwitch(
        val id: String,
    ) : LuxUiEvent

    data class PickOption(
        val optionId: String,
        val value: String,
    ) : LuxUiEvent

    data class SetNick(
        val nick: String,
    ) : LuxUiEvent

    data class SetAccountMode(
        val mode: String,
    ) : LuxUiEvent

    data object DemoLogin : LuxUiEvent

    data class SetRoomLayout(
        val layout: String,
    ) : LuxUiEvent

    data class SetVersionModal(
        val open: Boolean,
    ) : LuxUiEvent

    data class SetStatsPeriod(
        val period: String,
    ) : LuxUiEvent

    data class SetStatsChartKind(
        val kind: String,
    ) : LuxUiEvent

    data class ShowToast(
        val message: String,
    ) : LuxUiEvent

    data object ClearToast : LuxUiEvent
}
