with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

widget_xml = """        <receiver
            android:name=".widget.MusicWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
                <action android:name="com.example.widget.ACTION_PLAY_PAUSE" />
                <action android:name="com.example.widget.ACTION_NEXT" />
                <action android:name="com.example.widget.ACTION_PREV" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/music_widget_info" />
        </receiver>
"""

content = content.replace(
    '    </application>',
    widget_xml + '    </application>'
)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
