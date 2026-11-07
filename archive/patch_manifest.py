import re
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

target = r'''        <service
            android:name="\.PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>'''

repl = r'''        <service
            android:name=".PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <meta-data
                android:name="androidx.media3.session.notification.small_icon"
                android:resource="@drawable/ic_launcher_foreground" />
            <intent-filter>'''

content = re.sub(target, repl, content)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
