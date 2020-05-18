package cx.aphex.dj_pipeline

import java.io.File

val REMOVE_THESE = listOf(
        " - Extended Mix",
        " (Extended Mix)",
        "(Extended Mix)"
)

fun main(args: Array<String>) {
    args.forEach {
        val file = File(it)

        val tracks = mutableListOf<String>()
        file.forEachLine { line ->
            if (line.startsWith("#EXTINF")) {
                var track = line.split(',', limit = 2)[1]
                REMOVE_THESE.forEach {
                    track = track.replace(it, "")
                }

                tracks.add(track)
            }
        }

        with(File("tracklist.txt")) {
            createNewFile()
            val writer = printWriter()
            tracks.forEachIndexed { index, s ->
                writer.println("${index + 1}. $s")
            }
            writer.close()
        }
    }
}
