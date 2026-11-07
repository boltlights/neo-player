import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target = r'''    onShufflePlay: \(\) -> Unit,
    onRemoveTrack: \(\(TrackEntity\) -> Unit\),
    onShowProperties: \(TrackEntity\) -> Unit,'''

target2 = r'''    onShufflePlay: \(\) -> Unit,
    onRemoveTrack: \(TrackEntity\) -> Unit,
    onShowProperties: \(TrackEntity\) -> Unit,'''

repl = r'''    onShufflePlay: () -> Unit,
    onRemoveTrack: (TrackEntity) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,'''

# let's just make sure it's there. Oh wait, my patch DID have `onRemoveTrack`. Let's check `PlaylistsScreen.kt`.
