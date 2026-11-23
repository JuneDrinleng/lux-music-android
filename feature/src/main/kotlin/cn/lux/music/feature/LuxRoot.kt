package cn.lux.music.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxSurface
import cn.lux.music.designsystem.LuxToast
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxTab
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiReducer
import cn.lux.music.feature.state.LuxUiState
import cn.lux.music.feature.ui.AccountScreen
import cn.lux.music.feature.ui.AddToPlaylistScreen
import cn.lux.music.feature.ui.CacheScreen
import cn.lux.music.feature.ui.CustomSrcScreen
import cn.lux.music.feature.ui.DeletePlaylistScreen
import cn.lux.music.feature.ui.DiscoverTabScreen
import cn.lux.music.feature.ui.FxScreen
import cn.lux.music.feature.ui.HomeTabScreen
import cn.lux.music.feature.ui.ListDetailScreen
import cn.lux.music.feature.ui.LuxBottomDock
import cn.lux.music.feature.ui.MineTabScreen
import cn.lux.music.feature.ui.NewPlaylistScreen
import cn.lux.music.feature.ui.OptionScreen
import cn.lux.music.feature.ui.PlayerOverlay
import cn.lux.music.feature.ui.QueueOverlay
import cn.lux.music.feature.ui.RenamePlaylistScreen
import cn.lux.music.feature.ui.RoamTabScreen
import cn.lux.music.feature.ui.RoomLayoutScreen
import cn.lux.music.feature.ui.SearchScreen
import cn.lux.music.feature.ui.SettingsScreen
import cn.lux.music.feature.ui.SimpleActionSubScreen
import cn.lux.music.feature.ui.SimpleSubSpec
import cn.lux.music.feature.ui.SmartPlaylistScreen
import cn.lux.music.feature.ui.StatsScreen
import cn.lux.music.feature.ui.SyncScreen
import cn.lux.music.feature.ui.TogetherScreen
import cn.lux.music.feature.ui.VersionScreen
import kotlinx.coroutines.delay

/**
 * Full 1:1 Compose implementation of the `ui/index.html` (`netease-lux` + `lux-earth-palette`)
 * shell, tabs, subpages, player, lyrics, comments, queue, and deterministic preview interactions.
 */
@Composable
public fun LuxRoot(onRegisterBackConsumer: ((() -> Boolean) -> Unit)? = null) {
    var state by remember { mutableStateOf(LuxUiState()) }
    val dispatch: (LuxUiEvent) -> Unit = { event -> state = LuxUiReducer.reduce(state, event) }

    LaunchedEffect(onRegisterBackConsumer) {
        onRegisterBackConsumer?.invoke {
            val handled = state.queueOpen || state.versionModalOpen || state.playerOpen || state.stack.isNotEmpty()
            if (handled) dispatch(LuxUiEvent.SystemBack)
            handled
        }
    }

    LaunchedEffect(state.playing) {
        if (!state.playing) return@LaunchedEffect
        while (true) {
            delay(250L)
            dispatch(LuxUiEvent.Tick(0.25))
        }
    }

    LaunchedEffect(state.toastSeq) {
        if (state.toastMessage == null) return@LaunchedEffect
        delay(1800L)
        dispatch(LuxUiEvent.ClearToast)
    }

    val tabScrolls = remember { LuxTab.entries.associateWith { ScrollState(0) } }
    LuxTab.entries.forEach { tab ->
        val epoch = state.scrollToTopEpoch[tab] ?: 0
        LaunchedEffect(epoch) {
            if (epoch > 0) tabScrolls.getValue(tab).animateScrollTo(0)
        }
    }

    LuxSurface {
        MainTabHost(state = state, scrollStates = tabScrolls, onEvent = dispatch)
        SubPageStackHost(state = state, onEvent = dispatch)
        LuxBottomDock(state = state, onEvent = dispatch)
        PlayerOverlay(state = state, onEvent = dispatch)
        QueueOverlay(state = state, onEvent = dispatch)
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
        ) {
            LuxToast(message = state.toastMessage)
        }
    }
}

@Composable
private fun MainTabHost(
    state: LuxUiState,
    scrollStates: Map<LuxTab, ScrollState>,
    onEvent: (LuxUiEvent) -> Unit,
) {
    when (state.tab) {
        LuxTab.Home -> HomeTabScreen(state, scrollStates.getValue(LuxTab.Home), onEvent)
        LuxTab.Discover -> DiscoverTabScreen(state, scrollStates.getValue(LuxTab.Discover), onEvent)
        LuxTab.Roam -> RoamTabScreen(state, scrollStates.getValue(LuxTab.Roam), onEvent)
        LuxTab.Mine -> MineTabScreen(state, scrollStates.getValue(LuxTab.Mine), onEvent)
    }
}

@Composable
private fun SubPageStackHost(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val topPage = state.stack.lastOrNull()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    AnimatedVisibility(
        visible = topPage != null,
        enter =
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(320, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
            ),
        exit =
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(260),
            ),
    ) {
        if (topPage != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(LuxColors.Page).padding(top = topInset),
            ) {
                SubPageContent(page = topPage, state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun SubPageContent(
    page: LuxSubPage,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    when (page) {
        is LuxSubPage.ListDetail -> ListDetailScreen(page.key, state, onEvent)
        LuxSubPage.Search -> SearchScreen(state, onEvent)
        LuxSubPage.Settings -> SettingsScreen(state, onEvent)
        is LuxSubPage.Option -> OptionScreen(page.id, state, onEvent)
        LuxSubPage.Account -> AccountScreen(state, onEvent)
        LuxSubPage.Stats -> StatsScreen(state, onEvent)
        LuxSubPage.NewPlaylist -> NewPlaylistScreen(state, onEvent)
        LuxSubPage.SmartPlaylist -> SmartPlaylistScreen(state, onEvent)
        is LuxSubPage.RenamePlaylist -> RenamePlaylistScreen(page.playlistId, state, onEvent)
        is LuxSubPage.DeletePlaylist -> DeletePlaylistScreen(page.playlistId, state, onEvent)
        is LuxSubPage.AddToPlaylist -> AddToPlaylistScreen(page.songId, state, onEvent)
        else -> MiscSubPageContent(page = page, state = state, onEvent = onEvent)
    }
}

@Composable
private fun MiscSubPageContent(
    page: LuxSubPage,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    when (page) {
        LuxSubPage.Together -> TogetherScreen(onEvent)
        LuxSubPage.Cache -> CacheScreen(onEvent)
        LuxSubPage.Sync -> SyncScreen(state, onEvent)
        LuxSubPage.Version -> VersionScreen(state, onEvent)
        LuxSubPage.Fx -> FxScreen(state, onEvent)
        LuxSubPage.CustomSrc -> CustomSrcScreen(onEvent)
        LuxSubPage.Room -> RoomLayoutScreen(state, onEvent)
        LuxSubPage.Conflict ->
            SimpleActionSubScreen(
                SimpleSubSpec("同步冲突策略", "当前没有已记住的冲突策略。", "清除已记住的策略", LuxButtonStyle.Tonal, "已清除记住的冲突策略（预览）"),
                onEvent,
            )
        LuxSubPage.Export ->
            SimpleActionSubScreen(
                SimpleSubSpec("导出数据", "预览中不会生成文件。", "导出备份", LuxButtonStyle.Primary, "已准备导出（预览）"),
                onEvent,
            )
        LuxSubPage.Import ->
            SimpleActionSubScreen(
                SimpleSubSpec("导入数据", "预览中不会读取文件。", "选择备份", LuxButtonStyle.Primary, "导入（预览中不读取文件）"),
                onEvent,
            )
        LuxSubPage.Github ->
            SimpleActionSubScreen(SimpleSubSpec("GitHub Releases", "预览为离线页面，不会打开外部链接。"), onEvent)
        else -> Unit
    }
}
