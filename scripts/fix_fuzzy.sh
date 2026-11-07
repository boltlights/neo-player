#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun fuzzySearchLocalTracks/ {
    print "    suspend fun fuzzySearchLocalTracks(allTracksList: List<TrackEntity>, query: String, limit: Int = 20): List<TrackEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {"
    print "        if (query.isBlank()) return@withContext emptyList()"
    print "        allTracksList.map {"
    print "            val titleScore = com.example.util.LevenshteinDistance.compute(query.lowercase(), it.title.lowercase())"
    print "            val artistScore = com.example.util.LevenshteinDistance.compute(query.lowercase(), it.artist.lowercase())"
    print "            it to kotlin.math.min(titleScore, artistScore)"
    print "        }.sortedBy { it.second }.take(limit).map { it.first }"
    print "    }"
    
    # skip original lines
    skip = 1
    next
}
skip && /private fun coerceFuzzyRange/ {
    skip = 2
    next
}
skip == 1 && /^    \}/ {
    skip = 0
    next
}
skip == 2 && /^    \}/ {
    skip = 0
    next
}
!skip { print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/data/MusicRepository.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/MusicRepository.kt
