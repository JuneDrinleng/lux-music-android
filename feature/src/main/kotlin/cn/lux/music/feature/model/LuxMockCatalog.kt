package cn.lux.music.feature.model

internal object LuxMockCatalog {
    val sources: Map<String, String> = mapOf("kw" to "酷我", "wy" to "网易", "qq" to "QQ")

    val searchSources: List<Pair<String, String>> =
        listOf("all" to "全部", "kw" to "酷我", "wy" to "网易", "qq" to "QQ")

    val hotWords: List<String> =
        listOf("晚风信箱", "Slow Tide", "月岛 Rin", "屋顶花园", "一万次日落", "夜航")

    val initialHistory: List<String> =
        listOf("晚风", "月岛 Rin", "Slow Tide", "屋顶花园")

    val localIds: List<String> = listOf("s14", "s15", "s16", "s18", "s20")

    val recentIds: List<String> = listOf("s1", "s5", "s9", "s3", "s13", "s7", "s11")

    val boards: List<PreviewBoard> =
        listOf(
            PreviewBoard("new", "新歌榜", 1),
            PreviewBoard("up", "飙升榜", 2),
            PreviewBoard("hot", "热歌榜", 3),
            PreviewBoard("jp", "日系清新榜", 5),
        )

    val songs: List<PreviewSong> =
        listOf(
            PreviewSong("s1", "晚风信箱", "林栖", "晚风信箱", 222, "c1", "zh", listOf("kw", "wy")),
            PreviewSong("s2", "薄荷色的周二", "林栖", "晚风信箱", 198, "c3", "zh", listOf("kw", "qq")),
            PreviewSong("s3", "灯塔下的猫", "周以宁", "海边的房间", 245, "c2", "zh", listOf("wy", "qq")),
            PreviewSong("s4", "南方没有雪", "周以宁", "海边的房间", 231, "c6", "zh", listOf("kw", "wy", "qq")),
            PreviewSong("s5", "星屑ステーション", "月岛 Rin", "Night Platform", 216, "c8", "ja", listOf("wy", "qq")),
            PreviewSong("s6", "青い自転車", "月岛 Rin", "Night Platform", 252, "c13", "ja", listOf("kw", "wy")),
            PreviewSong(
                "s7",
                "Paper Satellites",
                "The Quiet Harbor",
                "Low Orbit",
                208,
                "c12",
                "en",
                listOf("kw", "qq"),
            ),
            PreviewSong("s8", "Slow Tide", "The Quiet Harbor", "Low Orbit", 280, "c11", "en", listOf("wy")),
            PreviewSong("s9", "七月的海盐汽水", "白杨乐队", "夏天备忘录", 213, "c5", "zh", listOf("kw", "wy", "qq")),
            PreviewSong("s10", "地铁末班车", "白杨乐队", "夏天备忘录", 241, "c9", "zh", listOf("kw", "qq")),
            PreviewSong("s11", "旧相机", "苏和", "底片", 227, "c4", "zh", listOf("wy", "qq")),
            PreviewSong("s12", "屋顶花园", "苏和 / 林栖", "底片", 202, "c7", "zh", listOf("kw", "wy")),
            PreviewSong("s13", "雨のち晴れ", "小春日和", "天気予報", 235, "c14", "ja", listOf("kw", "wy", "qq")),
            PreviewSong("s14", "拾光", "陈默", "拾光", 260, "c10", "zh", listOf("qq")),
            PreviewSong("s15", "一万次日落", "陈默", "拾光", 224, "c12", "zh", listOf("kw", "wy")),
            PreviewSong("s16", "风筝与线", "方屿", "远处", 210, "c6", "zh", listOf("kw", "qq")),
            PreviewSong("s17", "Northbound", "Mira Vale", "Northbound", 195, "c13", "en", listOf("wy", "qq")),
            PreviewSong("s18", "纸飞机航线", "方屿", "远处", 238, "c3", "zh", listOf("kw", "wy", "qq")),
            PreviewSong("s19", "夜航", "林栖", "夜航", 255, "c1", "zh", listOf("wy")),
            PreviewSong("s20", "慢慢来", "周以宁", "慢慢来", 206, "c7", "zh", listOf("kw", "qq")),
        )

    val songById: Map<String, PreviewSong> = songs.associateBy { it.id }

    val initialPlaylists: List<PreviewPlaylist> =
        listOf(
            PreviewPlaylist("p_like_a1", "我喜欢的音乐", null, true, listOf("s3", "s5", "s9", "s1", "s13")),
            PreviewPlaylist(
                "p1",
                "夜里循环",
                "c1",
                false,
                listOf("s1", "s19", "s4", "s8", "s11", "s3", "s16", "s20", "s6", "s12", "s14", "s2"),
            ),
            PreviewPlaylist("p2", "通勤路上", "c13", false, listOf("s6", "s9", "s17", "s5", "s7", "s10", "s18")),
            PreviewPlaylist("p3", "周末慢歌", "c11", false, listOf("s8", "s20", "s12", "s15")),
        )

    val hotPlaylists: List<PreviewHotPlaylist> =
        listOf(
            PreviewHotPlaylist("h1", "深夜电台精选", "kw", "c1", listOf("s1", "s19", "s8", "s11", "s3", "s4", "s16", "s20")),
            PreviewHotPlaylist("h2", "通勤路上的日系", "wy", "c13", listOf("s5", "s6", "s13", "s17", "s9")),
            PreviewHotPlaylist("h3", "下午四点的咖啡馆", "qq", "c4", listOf("s2", "s12", "s7", "s14", "s15", "s18")),
            PreviewHotPlaylist("h4", "夏天的汽水", "kw", "c5", listOf("s9", "s10", "s5", "s7", "s2")),
        )

    val hotComments: List<PreviewComment> =
        listOf(
            PreviewComment("0", "阿栗", "2 小时前", "通勤路上循环第三遍，副歌一出来整个车厢都安静了。", 128),
            PreviewComment("1", "Mori", "昨天", "这段间奏像下过雨的街道，很干净。", 64),
            PreviewComment("2", "小鹿不迷路", "3 天前", "歌词里“收起伞一起走吧”太温柔了。", 52),
            PreviewComment("3", "eleven", "上周", "被推荐到这首，意外地好听。", 19),
            PreviewComment("4", "周末慢跑", "上周", "慢跑配速刚好，推荐给大家。", 8),
        )

    val newComments: List<PreviewComment> =
        listOf(
            PreviewComment("n0", "夜跑的鱼", "刚刚", "第一次听，前奏就很喜欢。", 0),
            PreviewComment("n1", "阿白", "5 分钟前", "收藏了，明天通勤继续听。", 1),
            PreviewComment("n2", "Kiri", "1 小时前", "谁能推荐几首类似的？", 2),
        )

    val dailyIds: List<String> = pick(4, 10)
    val weeklyIds: List<String> = pick(6, 8)
    val radioIds: List<String> = pick(11, 16)

    fun pick(
        seed: Int,
        count: Int,
    ): List<String> {
        val out = mutableListOf<String>()
        val len = songs.size
        var i = 0
        while (out.size < count && i < 60) {
            val id = songs[(seed * 7 + i * 3) % len].id
            if (id !in out) out.add(id)
            i++
        }
        return out
    }

    fun sourceName(key: String): String = sources[key] ?: key

    fun playCountLabel(index: Int): String = listOf("12.8万", "3.4万", "9,812", "26.1万", "5.6万", "1.2万")[index % 6]

    fun formatTime(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        val mins = total / 60
        val secs = total % 60
        return "$mins:${secs.toString().padStart(2, '0')}"
    }
}
