#!/bin/bash
sed -i 's/repository.insertTrack(newTrack)/repository.insertTrack(newTrack)\n                            currentTrackList.add(newTrack)/g' app/src/main/java/com/example/util/MediaStoreManager.kt
