#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun fuzzySearchLocalTracks/ {
    print "    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {"
    print "        val lhsLength = lhs.length"
    print "        val rhsLength = rhs.length"
    print "        var cost = Array(lhsLength + 1) { it }"
    print "        var newCost = Array(lhsLength + 1) { 0 }"
    print "        for (i in 1..rhsLength) {"
    print "            newCost[0] = i"
    print "            for (j in 1..lhsLength) {"
    print "                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1"
    print "                val costReplace = cost[j - 1] + match"
    print "                val costInsert = cost[j] + 1"
    print "                val costDelete = newCost[j - 1] + 1"
    print "                newCost[j] = kotlin.math.min(kotlin.math.min(costInsert, costDelete), costReplace)"
    print "            }"
    print "            val swap = cost"
    print "            cost = newCost"
    print "            newCost = swap"
    print "        }"
    print "        return cost[lhsLength]"
    print "    }"
    print ""
    print "    suspend fun fuzzySearchLocalTracks(allTracksList: List<TrackEntity>, query: String, limit: Int = 20): List<TrackEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {"
    print "        if (query.isBlank()) return@withContext emptyList()"
    print "        allTracksList.map {"
    print "            val titleScore = levenshteinDistance(query.lowercase(), it.title.lowercase())"
    print "            val artistScore = levenshteinDistance(query.lowercase(), it.artist.lowercase())"
    print "            it to kotlin.math.min(titleScore, artistScore)"
    print "        }.sortedBy { it.second }.take(limit).map { it.first }"
    print "    }"
    
    skip = 1
    next
}
skip && /^    \}/ {
    skip = 0
    next
}
!skip { print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/data/MusicRepository.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/MusicRepository.kt
