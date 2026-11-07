#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/components/MosaicCoverArt.kt
package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun MosaicCoverArt(
    imageUris: List<String?>,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Gray,
    iconIfEmpty: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.MusicNote
) {
    val validUris = imageUris.filterNotNull().filter { it.isNotBlank() }.distinct().take(4)
    
    Box(modifier = modifier.background(Color(0xFF2E2E2E))) {
        if (validUris.isEmpty()) {
            Icon(
                imageVector = iconIfEmpty,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        } else if (validUris.size == 1) {
            Image(
                painter = rememberAsyncImagePainter(validUris.first()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (validUris.size == 2) {
            Row(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter(validUris[0]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Image(
                    painter = rememberAsyncImagePainter(validUris[1]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else if (validUris.size == 3) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(validUris[0]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Image(
                        painter = rememberAsyncImagePainter(validUris[1]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Image(
                    painter = rememberAsyncImagePainter(validUris[2]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(validUris[0]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Image(
                        painter = rememberAsyncImagePainter(validUris[1]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(validUris[2]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Image(
                        painter = rememberAsyncImagePainter(validUris[3]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
INNER_EOF
