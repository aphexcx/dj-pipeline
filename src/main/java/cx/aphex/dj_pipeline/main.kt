package cx.aphex.dj_pipeline


import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.NullTag
import java.io.File

val VALID_EXTENSIONS: Array<String> = arrayOf(
        "mp3", "aif", "aiff"
)

fun main(args: Array<String>) {
    args.forEach {
        val file = File(it)
        when {
            !file.exists() -> return@forEach
            file.isDirectory -> processDirectory(file)
            else -> processFile(file)
        }
    }
//    processFile("/Users/afik_cohen/IdeaProjects/dj-pipeline/Andy Moor, Diana Leah, Somna - There Is Light (Extended Mix).aiff")
}

private fun String.isValidExtension(): Boolean = this in VALID_EXTENSIONS

fun processDirectory(dir: File) {
    dir.walkBottomUp().forEach { processFile(it) }
}

fun processFile(file: File) {
    if (!file.extension.isValidExtension()) {
        return
    }
    val audioFile = AudioFileIO.read(file)

    val tag = audioFile.tag.or(NullTag.INSTANCE)
    if (tag == NullTag.INSTANCE) { // there was no tag
        throw Exception("No id3 tag present in %s".format(audioFile.file))
    }

    val comment = tag.getValue(FieldKey.COMMENT).or("")
    val sharpKey = tag.getValue(FieldKey.KEY).or("")
    val energyLevel = getEnergyLevel(tag.getValue(FieldKey.GROUPING).or(""))
    if (!comment.contains("-")) {
        println("ERROR: Comment tag not formatted in camelotKey - bpm format (or already processed): [$comment] %s".format(audioFile.file))
        return
    }
    getCamelotKeyAndBpm(comment).let { (camelotKey, bpm) ->
        val newComment = makeComment(
                makeCamelotKeyDoubleDigit(camelotKey),
                sharpKey.padEnd(3),
                bpm,
                energyLevel)
        tag.setField(FieldKey.COMMENT, newComment)
        audioFile.save()
    }
}

private fun getEnergyLevel(grouping: String): Int {
    return grouping.split("-").first().trim().toInt()
}

// Accepts a comment in the form 3A - 128 and returns the camelotKey and bpm
private fun getCamelotKeyAndBpm(comment: String): Pair<String, Double> =
        comment.split("-").map { it.trim() }.let { (camelotKey, bpmString) -> camelotKey to bpmString.toDouble() }

// Makes a comment in the form:
// 03A A#m 128 E7
private fun makeComment(
        camelotKey: String,
        sharpKey: String,
        bpm: Double,
        energyLevel: Int
): String =
        "$camelotKey $sharpKey $bpm E$energyLevel"

// Make camelot keys double digit, e.g. 1A -> 01A, 2B -> 02B, 11A -> 11A 4B/2A -> 04B/02A
private fun makeCamelotKeyDoubleDigit(comment: String): String {
    val regex = """(\d+)(A|B)""".toRegex()
    return regex.replace(comment) {
        val (digits, rest) = it.destructured
        String.format("%02d", digits.toInt()) + rest
    }
}
