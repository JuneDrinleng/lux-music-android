package cn.lux.music.feature.model

internal object LuxMockLyrics {
    private val bySong: Map<String, List<PreviewLyricLine>> =
        LuxMockLyricsPart1.entries + LuxMockLyricsPart2.entries

    fun forSong(songId: String): List<PreviewLyricLine> = bySong[songId].orEmpty()

    fun lineIndexAt(
        lines: List<PreviewLyricLine>,
        timeSec: Double,
    ): Int {
        var idx = 0
        for (k in lines.indices) {
            if (lines[k].timeSec <= timeSec) idx = k else break
        }
        return idx
    }
}
