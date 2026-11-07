#!/bin/bash
cat << 'INNER_EOF' > script.awk
/PlaybackHistoryEntity::class/ {
    print "        PlaybackHistoryEntity::class,"
    print "        SmartVolumeZoneEntity::class"
    next
}
/version = 10/ {
    print "    version = 11,"
    next
}
/val migrations = \(1 until 10\).map/ {
    print "                val migrations = (1 until 11).map { version ->"
    print "                    object : Migration(version, version + 1) {"
    print "                        override fun migrate(db: SupportSQLiteDatabase) {"
    print "                            if (version == 10) {"
    print "                                db.execSQL(\"CREATE TABLE IF NOT EXISTS `smart_volume_zones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radiusMeters` REAL NOT NULL, `targetVolumePercent` REAL NOT NULL, `isGradualFade` INTEGER NOT NULL, `isActive` INTEGER NOT NULL DEFAULT 1)\")"
    print "                            }"
    print "                        }"
    print "                    }"
    print "                }.toTypedArray()"
    next
}
{
    if ($0 ~ /object : Migration\(version, version \+ 1\)/) {
        skip = 1
    }
    if (skip == 1 && $0 ~ /}\.toTypedArray\(\)/) {
        skip = 0
        next
    }
    if (skip == 1) next
    print
}
INNER_EOF
awk -f script.awk app/src/main/java/com/example/data/MusicDatabase.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/MusicDatabase.kt
