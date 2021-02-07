package cx.aphex.dj_pipeline


import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.NullTag
import java.io.File

fun main(args: Array<String>) {
    args.forEach {
        val file = File(it)
        when {
            !file.exists() -> return@forEach
            file.isDirectory -> deemixProcessor.processDirectory(file)
            else -> deemixProcessor.processFile(file)
        }
    }
}

object deemixProcessor {
    fun processDirectory(dir: File) {
        dir.walkBottomUp().forEach { processFile(it) }
    }

    fun processFile(file: File) {
        if (!file.extension.isValidExtension()) {
            return
        }
        if (file.extension == "aiff") {
            return // Skip aiff because those are from beatport and known good
        }

        val audioFile = AudioFileIO.read(file)

        print("${audioFile.file} ")

        val tag = audioFile.tag.or(NullTag.INSTANCE)
        if (tag == NullTag.INSTANCE) { // there was no tag
            throw Exception("❌ No id3 tag present in %s".format(audioFile.file))
        }

        val artist = tag.getValue(FieldKey.ARTIST).orNull()
                ?: throw MissingTagFieldException(FieldKey.ARTIST)

        val artists = artist.split(", ")
        if (artists.size < 2) {
            println("🚸 Only one artist here, skipping: \"$artist\"")
            return
        }
        var ampersandArtist: String? = artists.filter { it.contains('&') }.maxBy { it.length }
        if (ampersandArtist == null) {
            ampersandArtist = artists.filter { it.contains("feat.") }.maxBy { it.length }
        }

        if (ampersandArtist == null) {
            println("🚸 No ampersanded or feat artists found here, skipping: \"$artist\"")
            return
        }
        val newArtists = mutableListOf<String>()

        for (a in artists) {
            if (a == ampersandArtist) {
                newArtists.add(ampersandArtist)
                continue
            }
            if (!ampersandArtist.toLowerCase().contains(a.toLowerCase())) {
                newArtists.add(a)
            }
        }

        val newArtistField = LinkedHashSet(newArtists.toList()).joinToString(", ")
        if (newArtistField == artist) {
            return //field is unchanged
        }

        println("✅ Fixing ampersanded/featured artists \"$artist\" with new field \"$newArtistField\"")

        // UNCOMMENT TO ACTUALLY SAVE
//        tag.setField(FieldKey.ARTIST, newArtistField)
//        audioFile.save()
//
//
//        println("✅")

        /** Other approach that keeps everything BUT the ampersanded artist, abandoned because too many edge cases where I did want to keep it .e.g 'feat.' 'and', 'vs.'
         *         val artists = artist.split(", ")
        if (artists.size < 2) {
        println("🚸 Only one artist here, skipping: \"$artist\"")
        return
        }
        val ampersandArtist: String? = artists.filter { it.contains('&') }.maxBy { it.length }
        if (ampersandArtist == null) {
        println("🚸 No ampersanded artists found here, skipping: \"$artist\"")
        return
        }
        val newArtists = mutableListOf<String>()

        var ampersandArtistRemaining: String = ampersandArtist //.toSet()
        for (a in artists) {
        if (a == ampersandArtist) {
        newArtists.add(ampersandArtist)
        continue
        }
        if (ampersandArtistRemaining.toLowerCase().contains(a.toLowerCase())) {
        ampersandArtistRemaining = ampersandArtistRemaining.replace(a,"", ignoreCase = true)
        }
        newArtists.add(a)
        }
        if (ampersandArtistRemaining.trim() == "&" || ampersandArtistRemaining.trim() == "and") {
        newArtists.remove(ampersandArtist)
        }

        val newArtistField = newArtists.toSet().toList().joinToString(", ")
        println("✅ Fixing ampersanded artists \"$artist\" with new field \"$newArtistField\"")

        //        tag.setField(FieldKey.ARTIST, newArtistField)
        //        audioFile.save()
        //        println("✅")
        }
         */
    }
}
