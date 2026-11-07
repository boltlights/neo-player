#!/bin/bash
sed -i 's/                        } else {/                        if (bitmap != null) {\n                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)\n                        } else {/g' app/src/main/java/com/example/widget/MusicWidgetProvider.kt
