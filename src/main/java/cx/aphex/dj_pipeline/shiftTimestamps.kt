package cx.aphex.dj_pipeline

import java.io.File
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val beginning = LocalTime.MIN //  LocalTime.parse("00:00",timeStampFormatter)
val dateformatter = DateTimeFormatter.ofPattern("H:mm:ss")
val shiftBy = "0:08:11"
val shiftByTime = LocalTime.parse(shiftBy, dateformatter)

fun main(args: Array<String>) {
    val file = File(args.first())

    val shiftedLines = mutableListOf<String>()
    file.forEachLine { line ->
        val splitLine = line.split(" ")
        val timestamp = splitLine.last()

        val timestampTime = LocalTime.parse(timestamp, dateformatter)
        val shiftedTime = timestampTime.minus(Duration.between(beginning, shiftByTime))

        shiftedLines.add("${splitLine.dropLast(1).joinToString(" ")} ${shiftedTime.format(dateformatter)}")
    }

    with(File("shiftedtracklist.txt")) {
        createNewFile()
        val writer = printWriter()
        shiftedLines.forEach { s ->
            writer.println(s)
        }
        writer.close()
    }
}
