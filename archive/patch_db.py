import re
with open('app/src/main/java/com/example/data/MusicDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace('version = 13', 'version = 14')

target = r'''                val migrations = \(1 until 13\)\.map \{ version ->'''
repl = r'''                val migrations = (1 until 14).map { version ->'''
content = re.sub(target, repl, content)

target2 = r'''                            if \(version == 12\) \{
                                db\.execSQL\("ALTER TABLE `playback_state` ADD COLUMN `baseQueueTrackIds` TEXT NOT NULL DEFAULT ''"\)
                                db\.execSQL\("ALTER TABLE `playback_state` ADD COLUMN `isRepeatEnabled` INTEGER NOT NULL DEFAULT 0"\)
                            \}
                        \}'''
repl2 = r'''                            if (version == 12) {
                                db.execSQL("ALTER TABLE `playback_state` ADD COLUMN `baseQueueTrackIds` TEXT NOT NULL DEFAULT ''")
                                db.execSQL("ALTER TABLE `playback_state` ADD COLUMN `isRepeatEnabled` INTEGER NOT NULL DEFAULT 0")
                            }
                            if (version == 13) {
                                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `coverArtUri` TEXT DEFAULT NULL")
                            }
                        }'''
content = re.sub(target2, repl2, content)

with open('app/src/main/java/com/example/data/MusicDatabase.kt', 'w') as f:
    f.write(content)
