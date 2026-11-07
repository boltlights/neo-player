with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

old_intent_filter = """            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService"/>
                <action android:name="androidx.media3.session.MediaLibraryService"/>
                <action android:name="android.media.browse.MediaBrowserService"/>
            </intent-filter>"""

new_intent_filter = """            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService"/>
                <action android:name="androidx.media3.session.MediaLibraryService"/>
                <action android:name="android.media.browse.MediaBrowserService"/>
                <action android:name="android.media.action.MEDIA_PLAY_FROM_SEARCH" />
            </intent-filter>"""

content = content.replace(old_intent_filter, new_intent_filter)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
