package com.sipun.sonora.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {
    @Test
    fun parsesSyncedLyrics() {
        val lyrics = LyricsParser.parse(
            "[00:01.20]First line\n[00:03.450]Second line\n[01:02.5]Third line"
        )
        assertEquals(1_200L, lyrics.lines[0].startMs)
        assertEquals("First line", lyrics.lines[0].text)
        assertEquals(3_450L, lyrics.lines[1].startMs)
        assertEquals(62_500L, lyrics.lines[2].startMs)
        assertEquals(true, lyrics.synced)
    }

    @Test
    fun groupsEnhancedWordTimestampsIntoReadableSyncedLines() {
        val lyrics = LyricsParser.parse(
            "<00:52.69>तो <00:53.01>फिर <00:53.66>आओ, <00:56.79>मुझको <00:57.764>सताओ"
        )
        assertEquals(listOf("तो फिर आओ,", "मुझको सताओ"), lyrics.lines.map(LyricsLine::text))
        assertEquals(listOf(52_690L, 56_790L), lyrics.lines.mapNotNull(LyricsLine::startMs))
        assertTrue(lyrics.synced)
    }

    @Test
    fun parsesPlainLyrics() {
        val lyrics = LyricsParser.parse("First line\n\nSecond line")
        assertEquals(listOf("First line", "Second line"), lyrics.lines.map(LyricsLine::text))
        assertEquals(false, lyrics.synced)
    }
}
