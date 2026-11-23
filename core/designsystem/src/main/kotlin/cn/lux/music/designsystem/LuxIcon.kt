package cn.lux.music.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** All 85 MingCute 3.1.0 icons bundled verbatim from `ui/index.html`. */
public enum class LuxIcon(
    @param:DrawableRes public val resId: Int,
) {
    Account(R.drawable.ic_lux_account),
    AccountMusic(R.drawable.ic_lux_account_music),
    AccountOn(R.drawable.ic_lux_account_on),
    Album(R.drawable.ic_lux_album),
    ArrowTR(R.drawable.ic_lux_arrow_t_r),
    Bell(R.drawable.ic_lux_bell),
    Bookmark(R.drawable.ic_lux_bookmark),
    Brief(R.drawable.ic_lux_brief),
    Broom(R.drawable.ic_lux_broom),
    Calendar(R.drawable.ic_lux_calendar),
    Cast(R.drawable.ic_lux_cast),
    ChartLine(R.drawable.ic_lux_chart_line),
    Check(R.drawable.ic_lux_check),
    ChevDown(R.drawable.ic_lux_chev_down),
    ChevLeft(R.drawable.ic_lux_chev_left),
    ChevRight(R.drawable.ic_lux_chev_right),
    Clock(R.drawable.ic_lux_clock),
    Close(R.drawable.ic_lux_close),
    Coffee(R.drawable.ic_lux_coffee),
    Cog(R.drawable.ic_lux_cog),
    CogOn(R.drawable.ic_lux_cog_on),
    Comment(R.drawable.ic_lux_comment),
    Compass(R.drawable.ic_lux_compass),
    CompassOn(R.drawable.ic_lux_compass_on),
    Creation(R.drawable.ic_lux_creation),
    Dash(R.drawable.ic_lux_dash),
    DashOn(R.drawable.ic_lux_dash_on),
    Dots(R.drawable.ic_lux_dots),
    Download(R.drawable.ic_lux_download),
    Equalizer(R.drawable.ic_lux_equalizer),
    Export(R.drawable.ic_lux_export),
    Fire(R.drawable.ic_lux_fire),
    Flash(R.drawable.ic_lux_flash),
    Grid(R.drawable.ic_lux_grid),
    Headphones(R.drawable.ic_lux_headphones),
    Heart(R.drawable.ic_lux_heart),
    HeartO(R.drawable.ic_lux_heart_o),
    History(R.drawable.ic_lux_history),
    Home(R.drawable.ic_lux_home),
    HomeOn(R.drawable.ic_lux_home_on),
    Info(R.drawable.ic_lux_info),
    Library(R.drawable.ic_lux_library),
    List(R.drawable.ic_lux_list),
    ListNum(R.drawable.ic_lux_list_num),
    Magnify(R.drawable.ic_lux_magnify),
    Mic(R.drawable.ic_lux_mic),
    Moon(R.drawable.ic_lux_moon),
    Music(R.drawable.ic_lux_music),
    MusicBox(R.drawable.ic_lux_music_box),
    MusicBoxOn(R.drawable.ic_lux_music_box_on),
    NewBox(R.drawable.ic_lux_new_box),
    Next(R.drawable.ic_lux_next),
    Palette(R.drawable.ic_lux_palette),
    Pause(R.drawable.ic_lux_pause),
    Pencil(R.drawable.ic_lux_pencil),
    PlAdd(R.drawable.ic_lux_pl_add),
    Play(R.drawable.ic_lux_play),
    PlaylistPlay(R.drawable.ic_lux_playlist_play),
    Plus(R.drawable.ic_lux_plus),
    Podcast(R.drawable.ic_lux_podcast),
    Prev(R.drawable.ic_lux_prev),
    Queue(R.drawable.ic_lux_queue),
    Radio(R.drawable.ic_lux_radio),
    RadioOn(R.drawable.ic_lux_radio_on),
    RadioTower(R.drawable.ic_lux_radio_tower),
    Repeat(R.drawable.ic_lux_repeat),
    RepeatOnce(R.drawable.ic_lux_repeat_once),
    Run(R.drawable.ic_lux_run),
    Shape(R.drawable.ic_lux_shape),
    Share(R.drawable.ic_lux_share),
    Shuffle(R.drawable.ic_lux_shuffle),
    Smile(R.drawable.ic_lux_smile),
    Sofa(R.drawable.ic_lux_sofa),
    Sort(R.drawable.ic_lux_sort),
    Speed(R.drawable.ic_lux_speed),
    Sync(R.drawable.ic_lux_sync),
    TextSearch(R.drawable.ic_lux_text_search),
    Thumb(R.drawable.ic_lux_thumb),
    ThumbOn(R.drawable.ic_lux_thumb_on),
    Timer(R.drawable.ic_lux_timer),
    Translate(R.drawable.ic_lux_translate),
    Trash(R.drawable.ic_lux_trash),
    Trending(R.drawable.ic_lux_trending),
    Update(R.drawable.ic_lux_update),
    Volume(R.drawable.ic_lux_volume),
}

/** Renders a MingCute 3.1.0 vector icon tinted with the specified color. */
@Composable
public fun LuxIconView(
    icon: LuxIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(icon.resId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
