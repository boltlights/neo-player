package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    accentColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sleep Timer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val options = listOf(15, 30, 45, 60)
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { minutes ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSetTimer(minutes)
                                        onDismiss()
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$minutes Min",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                        .clickable {
                            onSetEndOfTrack()
                            onDismiss()
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "End of Track",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable {
                            onCancelTimer()
                            onDismiss()
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel Timer",
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
