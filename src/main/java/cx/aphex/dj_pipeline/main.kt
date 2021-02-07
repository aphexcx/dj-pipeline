package cx.aphex.dj_pipeline


import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.NullTag
import java.io.File

val VALID_EXTENSIONS: Array<String> = arrayOf(
        "mp3", "aif", "aiff"
)

fun String.isValidExtension(): Boolean = this in VALID_EXTENSIONS

fun main(args: Array<String>) {
    args.forEach {
        val file = File(it)
        when {
            !file.exists() -> return@forEach
            file.isDirectory -> processDirectory(file)
            else -> processFile(file)
        }
    }
}

fun processDirectory(dir: File) {
    dir.walkBottomUp().forEach { processFile(it) }
}

fun processFile(file: File) {
    if (!file.extension.isValidExtension()) {
        return
    }
    val audioFile = AudioFileIO.read(file)

    print("${audioFile.file} ")

    val tag = audioFile.tag.or(NullTag.INSTANCE)
    if (tag == NullTag.INSTANCE) { // there was no tag
        throw Exception("❌ No id3 tag present in %s".format(audioFile.file))
    }

    val comment = tag.getValue(FieldKey.COMMENT).orNull()
            ?: throw MissingTagFieldException(FieldKey.COMMENT)
    val sharpKey = tag.getValue(FieldKey.KEY).orNull()
            ?: throw MissingTagFieldException(FieldKey.KEY)
    // Rekordbox doesn't actually write bpm to id3. Maybe figure out a different way to get rekordbox's bpm
//    val rekordboxBpm = tag.getValue(FieldKey.BPM).orNull()?.toDouble()
//            ?: throw MissingTagFieldException(FieldKey.BPM)

    val energyLevel = getEnergyLevel(tag.getValue(FieldKey.GROUPING).orNull()
            ?: throw MissingTagFieldException(FieldKey.GROUPING)
    )
    if (!comment.contains("-")) {
        println("🚸 WARN: Comment tag not formatted in camelotKey - bpm format (or already processed): \"$comment\"")
        return
    }
    getCamelotKeyAndBpm(comment).let { (camelotKey, bpm) ->
        val newComment = makeComment(
                makeCamelotKeyDoubleDigit(camelotKey),
                sharpKey,
                bpm,
                energyLevel)
        tag.setField(FieldKey.COMMENT, newComment)
        audioFile.save()
        println("✅")
    }
}

class MissingTagFieldException(fieldKey: FieldKey) : Exception("❌ ERROR: Missing tag field: [$fieldKey]")

private fun getEnergyLevel(grouping: String): Int {
    return grouping.split("-").first().trim().toInt()
}

// Accepts a comment in the form 3A - 128 and returns the camelotKey and bpm
private fun getCamelotKeyAndBpm(comment: String): Pair<String, Double> =
        comment.split("-").map { it.trim() }.let { (camelotKey, bpmString) -> camelotKey to bpmString.toDouble() }

// Makes a comment in the form:
// 03A 128 E7 A#m
private fun makeComment(
        camelotKey: String,
        sharpKey: String,
        bpm: Double,
        energyLevel: Int
): String =
        "$camelotKey ${"%.0f".format(bpm)} E$energyLevel $sharpKey"

// Make camelot keys double digit, e.g. 1A -> 01A, 2B -> 02B, 11A -> 11A 4B/2A -> 04B/02A
private fun makeCamelotKeyDoubleDigit(comment: String): String {
    val regex = """(\d+)(A|B)""".toRegex()
    return regex.replace(comment) {
        val (digits, rest) = it.destructured
        String.format("%02d", digits.toInt()) + rest
    }
}
