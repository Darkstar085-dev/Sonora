package com.sipun.sonora.data.lyrics

internal object LyricsParser {
    private val lrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]""")
    private val wordTimestamp = Regex("""<(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?>""")
    private val metadata = Regex(
        """^\s*\[(?:(?:ti|ar|al|by|re|ve|length|offset|kana|la|au|tool|id|sign)\s*:.*|x-[^]]+)\]$""",
        RegexOption.IGNORE_CASE,
    )
    private val voicePrefix = Regex("""^v\d+\s*:""", RegexOption.IGNORE_CASE)
    private val sentenceEnding = Regex("""[.!?।॥]$""")
    private const val WORD_GAP_LINE_BREAK_MS = 1_800L

    fun parse(value: String): Lyrics {
        val lines = value.lineSequence()
            .filterNot { metadata.matches(it) }
            .flatMap(::parseLine)
            .filter { it.text.isNotEmpty() }
            .toList()
        return Lyrics(lines, lines.any { it.startMs != null })
    }

    private fun parseLine(line: String): Sequence<LyricsLine> {
        val wordMatches = wordTimestamp.findAll(line).toList()
        if (wordMatches.isNotEmpty()) {
            val segments = wordMatches.mapIndexedNotNull { index, match ->
                val nextStart = wordMatches.getOrNull(index + 1)?.range?.first ?: line.length
                val text = line.substring(match.range.last + 1, nextStart)
                    .replace(wordTimestamp, "")
                    .trim()
                    .replace(voicePrefix, "")
                    .trim()
                val startMs = timestampToMs(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3],
                )
                text.takeIf(String::isNotEmpty)?.let { LyricsLine(it, startMs) }
            }
            if (segments.isEmpty()) return emptySequence()

            val grouped = mutableListOf<LyricsLine>()
            var currentText = StringBuilder()
            var currentStart: Long? = null
            var previousStart: Long? = null
            for (segment in segments) {
                val start = segment.startMs
                val gap = if (start != null && previousStart != null) start - previousStart else 0L
                val shouldBreak = currentText.isNotEmpty() &&
                    (gap > WORD_GAP_LINE_BREAK_MS || sentenceEnding.containsMatchIn(currentText.toString()))
                if (shouldBreak) {
                    grouped += LyricsLine(currentText.toString().trim(), currentStart)
                    currentText = StringBuilder()
                    currentStart = null
                }
                if (currentText.isNotEmpty()) currentText.append(' ')
                if (currentStart == null) currentStart = start
                currentText.append(segment.text)
                previousStart = start
            }
            if (currentText.isNotEmpty()) grouped += LyricsLine(currentText.toString().trim(), currentStart)
            return grouped.asSequence()
        }

        val matches = lrcTimestamp.findAll(line).toList()
        if (matches.isEmpty()) return sequenceOf(LyricsLine(line.trim()))
        return matches.map { match ->
            val text = line.substring(match.range.last + 1)
                .trim()
                .replace(voicePrefix, "")
                .trim()
            LyricsLine(
                text = text,
                startMs = timestampToMs(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3],
                ),
            )
        }.asSequence()
    }

    private fun timestampToMs(minutes: String, seconds: String, fraction: String): Long =
        minutes.toLong() * 60_000 +
            seconds.toLong() * 1_000 +
            when (fraction.length) {
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                3 -> fraction.toLong()
                else -> 0
            }
}
